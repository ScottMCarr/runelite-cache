/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreLocation
{
	private static final Logger logger = LoggerFactory.getLogger(StoreLocation.class);

	private static final int NUM_INDEXES = 16;
	private static final String TMP_DIR = "d:/temp";

	public static File LOCATION;
	private static File TMP;

	static
	{
		File tmp = new File(TMP_DIR);
		if (tmp.exists() || tmp.mkdir())
		{
			System.setProperty("java.io.tmpdir", TMP_DIR);
			TMP = tmp;
		}

		try
		{
			LOCATION = setupCacheDir();
		}
		catch (IOException ex)
		{
			logger.warn("unable to initialize cache tmp area", ex);
		}
	}

	private static Properties getProperies() throws IOException
	{
		Properties properties = new Properties();
		InputStream resourceAsStream = StoreLocation.class.getResourceAsStream("/cache.properties");
		properties.load(resourceAsStream);
		return properties;
	}

	private static File setupCacheDir() throws IOException
	{
		File file = new File(System.getProperty("java.io.tmpdir"), "cache-" + getProperies().getProperty("cache.version"));

		if (file.exists())
		{
			logger.info("Using preexisting cache working directory {}", file);
			return file;
		}

		file.mkdir();

		// Copy over files
		InputStream in = StoreLocation.class.getResourceAsStream("/main_file_cache.dat2");
		Files.copy(in, new File(file, "main_file_cache.dat2").toPath());

		in = StoreLocation.class.getResourceAsStream("/main_file_cache.idx255");
		Files.copy(in, new File(file, "main_file_cache.idx255").toPath());

		for (int i = 0; i <= NUM_INDEXES; ++i)
		{
			in = StoreLocation.class.getResourceAsStream("/main_file_cache.idx" + i);
			Files.copy(in, new File(file, "main_file_cache.idx" + i).toPath());
		}

		logger.info("Set up cache working directory to {}", file);

		return file;
	}

	public static TemporaryFolder getTemporaryFolder()
	{
		return new TemporaryFolder()
		{
			@Override
			public void after()
			{
				// don't cleanup if using local tmpdir
				if (TMP == null)
				{
					super.after();
				}
			}
		};
	}
}
