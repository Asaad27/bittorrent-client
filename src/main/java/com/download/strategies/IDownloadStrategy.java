package com.download.strategies;

public interface IDownloadStrategy {

	int updatePeerState();
	void clear();
	String getName();

}
