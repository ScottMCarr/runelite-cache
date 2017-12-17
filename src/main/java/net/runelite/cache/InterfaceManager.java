/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import net.runelite.cache.definitions.InterfaceDefinition;
import net.runelite.cache.definitions.exporters.InterfaceExporter;
import net.runelite.cache.definitions.loaders.InterfaceLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.cache.util.Namer;

public class InterfaceManager
{
	private final Store store;
	private final List<InterfaceDefinition> interfaces = new ArrayList<>();
	private final Namer namer = new Namer();

	public InterfaceManager(Store store)
	{
		this.store = store;
	}

	public void load() throws IOException
	{
		InterfaceLoader loader = new InterfaceLoader();

		Storage storage = store.getStorage();
		Index index = store.getIndex(IndexType.INTERFACES);

		for (Archive archive : index.getArchives())
		{
			int archiveId = archive.getArchiveId();
			byte[] archiveData = storage.loadArchive(archive);
			ArchiveFiles files = archive.getFiles(archiveData);

			for (FSFile file : files.getFiles())
			{
				int fileId = file.getFileId();

				int widgetId = (archiveId << 16) + fileId;

				InterfaceDefinition iface = loader.load(widgetId, file.getContents());
				interfaces.add(iface);
			}
		}
	}

	public List<InterfaceDefinition> getItems()
	{
		return interfaces;
	}

	public void export(File out) throws IOException
	{
		out.mkdirs();

		for (InterfaceDefinition def : interfaces)
		{
			InterfaceExporter exporter = new InterfaceExporter(def);

			File targ = new File(out, def.id + ".json");
			exporter.exportTo(targ);
		}
	}

	public void java(File java) throws IOException
	{
		System.setProperty("line.separator", "\n");
		java.mkdirs();
		File targ = new File(java, "InterfaceID.java");
		try (PrintWriter fw = new PrintWriter(targ))
		{
			fw.println("/* This file is automatically generated. Do not edit. */");
			fw.println("package net.runelite.api;");
			fw.println("");
			fw.println("public final class InterfaceID {");
			for (InterfaceDefinition def : interfaces)
			{
				if (def.name == null || def.name.equalsIgnoreCase("NULL"))
				{
					continue;
				}

				String name = namer.name(def.name, def.id);
				if (name == null)
				{
					continue;
				}

				fw.println("	public static final int " + name + " = " + def.id + ";");
			}
			fw.println("}");
		}
	}
}
