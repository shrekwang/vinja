"""Filesystem search requests and ripgrep JSON decoding; no Vim/UI dependencies."""

import base64
import fnmatch
import json
import os
import re


def make_request(node, tree, text):
    regex = text.startswith("/")
    if regex:
        if len(text) < 3 or not text.endswith("/"):
            raise ValueError("use /pattern/ for regex searches (pattern must not be empty)")
        text = text[1:-1]
    root = os.path.abspath(node.realpath)
    args = ["rg", "--no-config", "--json", "--line-buffered", "--color=never",
            "--max-filesize=2M", "--encoding", tree.project_encoding,
            "--glob=!.git", "--iglob=!*.{zip,jar,war,ear,rar}"]
    if tree.hide_dot_files:
        args.append("--glob=!CVS")
    else:
        args.append("--hidden")
    # Consume the rest of each line so rg JSON cannot emit millions of submatches.
    # The non-greedy prefix is intentionally absent: retain the first-match column.
    pattern = text if regex else re.escape(text)
    args.extend(["--regexp", "(?:" + pattern + ")[^\\n]*", "--", root])
    filters = {}
    for pattern in tree.work_path_set:
        filters.setdefault(os.path.dirname(pattern), []).append(pattern)
    return dict(argv=args, root=root, pattern=text, prefix=node.get_rel_path(),
                filters=filters)


def _visible(path, request):
    relative = os.path.relpath(path, request["root"])
    if relative == ".." or relative.startswith(".." + os.sep):
        return False
    path = request["prefix"]
    for section in relative.split(os.sep):
        path = path + "/" + section if path else section
        patterns = request["filters"].get(os.path.dirname(path), [])
        if patterns and not any(fnmatch.fnmatch(path, pattern) for pattern in patterns):
            return False
    return True


def _text(value):
    if "text" in value:
        return value["text"]
    return base64.b64decode(value["bytes"]).decode("utf-8", errors="replace")


class MatchParser:
    """Reuse for all batches of one search, then release to discard visibility."""

    def __init__(self, request):
        self.request = request
        self.visibility = {}

    def parse_matches(self, messages):
        items = []
        skipped = 0
        for message in messages:
            event = json.loads(message)
            if event["type"] != "match":
                continue
            data = event["data"]
            path_data = data["path"]
            if "text" in path_data:
                path = path_data["text"]
            else:
                # Vim's Python bridge cannot represent surrogate-escaped filenames.
                try:
                    path = base64.b64decode(path_data["bytes"]).decode("utf-8")
                except UnicodeDecodeError:
                    skipped += 1
                    continue
            if path not in self.visibility:
                self.visibility[path] = _visible(path, self.request)
            if not self.visibility[path]:
                continue
            matches = data["submatches"]
            items.append(dict(filename=path, lnum=data["line_number"],
                              col=matches[0]["start"] + 1 if matches else 1,
                              text=_text(data["lines"]).rstrip("\r\n")[:500]))
        return dict(items=items, skipped=skipped)


def parse_matches(messages, request):
    """Parse a single batch without retaining state between calls."""
    return MatchParser(request).parse_matches(messages)
