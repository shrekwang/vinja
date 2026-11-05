from pathlib import Path
import runpy
import sqlite3
import sys
import types
import unittest
from unittest import mock


REPO = Path(__file__).resolve().parents[2]


class SqliteTableTests(unittest.TestCase):
    def setUp(self):
        vim = types.ModuleType("vim")
        vim.eval = mock.Mock(return_value="1")
        with mock.patch.dict(sys.modules, vim=vim):
            module = runpy.run_path(str(REPO / "vinja/python/dbext.py"))
        self.query_util = module["QueryUtil"]
        self.db = module["Dbext"]()
        scope = self.query_util.queryTablesByName.__globals__
        scope["dbext"] = self.db
        scope["MiscUtil"] = types.SimpleNamespace(displayWidth=len)
        self.output = scope["output"] = mock.Mock()
        self.conn = sqlite3.connect(":memory:")
        self.addCleanup(self.conn.close)
        self.conn.executescript('''
            CREATE TABLE users (id INTEGER);
            CREATE TABLE orders (id INTEGER);
            CREATE TABLE "owner's notes" (id INTEGER);
            CREATE VIEW user_view AS SELECT * FROM users;
        ''')
        self.db.getDbOption = mock.Mock(return_value={"servertype": "sqlite", "file": ":memory:"})
        self.db.createConn = mock.Mock(return_value=self.conn)

    def test_list_tables_renders_query_output(self):
        output_buffer = []
        with mock.patch.object(type(self.db), "getOutputBuffer", return_value=output_buffer):
            self.query_util.queryTables()
        result, buffer = self.output.call_args.args
        self.assertIs(output_buffer, buffer)
        self.assertEqual(self.db.format(["table_name"], [
            ("orders",), ("owner's notes",), ("users",),
        ]), result)

    def test_filter_tables_by_name(self):
        for name, expected in (("USER", [("users",)]),
                               ("owner's", [("owner's notes",)]),
                               ("missing", [])):
            with self.subTest(name=name):
                columns, rows = self.query_util.queryTablesByName(name)
                self.assertEqual(["table_name"], columns)
                self.assertEqual(expected, rows)


if __name__ == "__main__":
    unittest.main()
