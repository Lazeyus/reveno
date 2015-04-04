/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reveno.atp.api.exceptions.IllegalFileName;

/*
 * File name pattern:
 * 
 * {prefix}-{date}-{version}
 * 
 * Example:
 * 
 * /var/transactions/tx-2015_05_21-14
 * 
 */
public abstract class VersionedFileUtils {

	public static String nextVersionFile(File baseDir, String prefix) {
		return nextVersionFile(baseDir, prefix, null);
	}
	
	public static String nextVersionFile(File baseDir, String prefix, String version) { 
		Optional<String> lastFile = listFiles(baseDir, prefix, true).stream().reduce((a,b)->b);
		
		Function<Long, String> nextFile = (v) -> String.format("%s-%s-%s", prefix, FORMAT.format(new Date()),
				version == null ? v + 1 : version);
		if (!lastFile.isPresent()) {
			return nextFile.apply(0L);
		} else {
			VersionedFile file = parseVersionedFile(lastFile.get());
			if (daysBetween(file.getFileDate(), now()) >= 0) {
				return nextFile.apply(file.getVersion());
			} else {
				throw new RuntimeException("Your system clock is out of sync with transaction data. Please check it.");
			}
		}
	}
	
	public static Optional<String> lastVersionFile(File baseDir, String prefix) {
		return listFiles(baseDir, prefix, false).stream().reduce((a,b)->b);
	}
	
	public static VersionedFile lastVersionedFile(File baseDir, String prefix) {
		Optional<String> o = lastVersionFile(baseDir, prefix);
		if (o.isPresent())
			return parseVersionedFile(o.get());
		else
			return null;
	}
	
	public static VersionedFile parseVersionedFile(String fileName) {
		String[] parts = fileName.split("-");
		if (parts.length != 3) 
			throw new IllegalFileName(fileName);
		
		try {
			Calendar c = Calendar.getInstance();
			c.setTime(FORMAT.parse(parts[1]));
			return new VersionedFile(fileName, parts[0], c, Long.parseLong(parts[2]));
		} catch (Throwable t) {
			throw new IllegalFileName(fileName);
		}
	}
	
	public static List<String> listFolders(File baseDir, String prefix) {
		try {
			return Files.list(baseDir.toPath())
				.map(p -> p.toFile())
				.filter(File::isDirectory)
				.map(f -> f.getName())
				.filter(fn -> fn.startsWith(prefix))
				.sorted()
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<String> listFiles(File baseDir, String prefix, boolean listToday) {
		try {
			return Files.list(baseDir.toPath())
				.map(p -> p.toFile())
				.filter(File::isFile)
				.map(f -> f.getName())
				.filter(fn -> fn.startsWith(prefix + (listToday ? "-" + FORMAT.format(new Date()) : "")))
				.sorted()
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<VersionedFile> listVersioned(File baseDir, String prefix) {
		return listFiles(baseDir, prefix, false).stream().map(s -> parseVersionedFile(s)).collect(Collectors.toList());
	}
	
	protected static long daysBetween(Calendar startDate, Calendar endDate) {
	    long end = endDate.getTimeInMillis();
	    long start = startDate.getTimeInMillis();
	    return TimeUnit.MILLISECONDS.toDays(Math.abs(end - start));
	}
	
	protected static Calendar now() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		return c;
	}
	
	
	public static class VersionedFile {
		private String name;
		public String getName() {
			return name;
		}
		
		private String prefix;
		public String getPrefix() {
			return prefix;
		}
		
		private Calendar fileDate;
		public Calendar getFileDate() {
			return fileDate;
		}
		
		private long version;
		public long getVersion() {
			return version;
		}
		
		public VersionedFile(String name, String prefix, Calendar fileDate, long version) {
			this.name = name;
			this.prefix = prefix;
			this.fileDate = fileDate;
			this.version = version;
		}
		
		@Override
		public String toString() {
			return String.format("[%s,%s]", name, version);
		}
	}
	
	
	protected static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy_MM_dd");
	
}