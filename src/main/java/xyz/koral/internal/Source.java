package xyz.koral.internal;

import java.io.InputStream;
import java.nio.file.Path;

public interface Source
{
	InputStream createInputStream();
	InputStream createInputStream(long offset, long bytes);
	Path getFile();
}
