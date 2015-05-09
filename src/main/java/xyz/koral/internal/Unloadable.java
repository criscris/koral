package xyz.koral.internal;

public interface Unloadable
{
	long memorySize();
	void unload();
}
