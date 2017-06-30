package com.github.vinja.parser;

import java.util.concurrent.FutureTask;

public class VinjaSourceSearcherLoaderFutureTask extends FutureTask<VinjaJavaSourceSearcher> implements Comparable<VinjaSourceSearcherLoaderFutureTask> {

	private VinjaSourceSearcherLoader task = null;

	public  VinjaSourceSearcherLoaderFutureTask(VinjaSourceSearcherLoader task){
		super(task);
		this.task = task;
	}

	@Override
	public int compareTo(VinjaSourceSearcherLoaderFutureTask another) {
		return task.getPriority() - another.task.getPriority();
	}
}
