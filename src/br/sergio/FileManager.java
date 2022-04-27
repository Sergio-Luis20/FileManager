package br.sergio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public final class FileManager {
	
	private FileManager() {}
	
	public static Object readObject(File file) throws IOException, ClassNotFoundException {
		if(!file.exists()) {
			return null;
		}
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream obj = new ObjectInputStream(fis);
		Serializable imported = (Serializable) obj.readObject();
		obj.close();
		return imported;
	}
	
	public static void writeObject(File file, Serializable serializable) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream obj = new ObjectOutputStream(fos);
		obj.writeObject(serializable);
		obj.close();
	}
	
	public static void overwriteText(File file, String text) throws IOException {
		assertExistence(file);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(text);
		}
	}
	
	public static void appendText(File file, String text) throws IOException {
		appendText(file, text, true);
	}
	
	public static void appendText(File file, String text, boolean skipLine) throws IOException {
		appendText(file, text, skipLine, true);
	}
	
	public static void appendText(File file, String text, boolean skipLine, boolean duplicate) throws IOException {
		assertExistence(file);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
			String append;
			boolean clone;
			if(lines(file).isEmpty()) {
				append = text;
				clone = false;
			} else {
				append = skipLine ? "\n" + text : text;
				clone = readText(file).contains(text) && !duplicate;
			}
			writer.write(clone ? "" : append);
		}
	}
	
	public static void overwriteLines(File file, List<String> lines) throws IOException {
		overwriteText(file, toTextLines(lines));
	}
	
	public static void appendLines(File file, List<String> lines) throws IOException {
		appendText(file, toTextLines(lines));
	}
	
	public static String readLine(File file, int index) throws IOException {
		try {
			return lines(file).get(index);
		} catch(IndexOutOfBoundsException e) {
			if(e.getMessage().equals("Index: 0, Size: 0")) {
				return "";
			} else {
				throw e;
			}
		}
	}
	
	public static List<String> lines(File file) throws IOException {
		try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
			return reader.lines().collect(Collectors.toList());
		}
	}
	
	public static String readText(File file) throws IOException {
		byte[] array = readBytes(file);
		return array == null ? null : new String(readBytes(file));
	}
	
	public static void setLine(File file, int index, String newLine) throws IOException {
		String text = newLine == null ? "" : newLine;
		List<String> lines = lines(file);
		int size = lines.size();
		if(index < 0 || index >= size) {
			throw new IllegalArgumentException("O índice da linha não pode ser menor que 0 ou maior"
					+ " ou igual à quantidade de linhas do arquivo.");
		}
		lines.set(index, text);
		for(int i = 0; i < size; i++) {
			if(i == 0) {
				overwriteText(file, lines.get(i));
			} else {
				appendText(file, lines.get(i), true);
			}
		}
	}
	
	public static void removeLine(File file, int index) throws IOException {
		List<String> lines = lines(file);
		int size = lines.size();
		if(index < 0 || index >= size) {
			throw new IllegalArgumentException("O índice da linha não pode ser menor que 0 ou maior"
					+ " ou igual à quantidade de linhas do arquivo.");
		}
		lines.remove(index);
		--size;
		for(int i = 0; i < size; i++) {
			if(i == 0) {
				overwriteText(file, lines.get(i));
			} else {
				appendText(file, lines.get(i), true);
			}
		}
	}
	
	public static int getIndex(File file, String text) throws IOException {
		if(text == null) {
			return -1;
		}
		List<String> lines = lines(file);
		int hash = text.hashCode();
		for(String line : lines) {
			if(line.hashCode() == hash) {
				if(line.equals(text)) {
					return lines.indexOf(line);
				}
			}
		}
		return -1;
	}
	
	public static synchronized void copyDirectoryToDirectory(File srcDir, File destDir, 
			boolean replaceIfExists, boolean preserveFilesDate) throws IOException {
		if(srcDir.isFile() || destDir.isFile()) {
			throw new IOException("Os parâmetros do tipo File não podem ser arquivos.");
		}
		File newDir = new File(destDir, srcDir.getName());
		if(!deleteIfExists(replaceIfExists, newDir)) {
			return;
		}
		newDir.mkdir();
		File[] subFiles = srcDir.listFiles();
		for(File file : subFiles) {
			File copy = new File(newDir, file.getName());
			if(file.isFile()) {
				copyFileToFile(file, copy, true, preserveFilesDate);
			} else {
				copyDirectoryToDirectory(file, newDir, true, preserveFilesDate);
			}
		}
		if(preserveFilesDate) {
			destDir.setLastModified(srcDir.lastModified());
		}
	}
	
	public static synchronized void copyFileToDirectory(File file, File directory, 
			boolean replaceIfExists, boolean preserveFileDate) throws IOException {
		FileManager.assertExistence(directory, true);
		if(file.isDirectory()) {
			throw new IOException("O parâmetro file não pode ser um diretório.");
		}
		if(directory.isFile()) {
			throw new IOException("O parâmetro directory não pode ser um arquivo.");
		}
		File copy = new File(directory, file.getName());
		if(!deleteIfExists(replaceIfExists, copy)) {
			return;
		}
		copy.createNewFile();
		writeBytes(copy, readBytes(file));
		if(preserveFileDate) {
			copy.setLastModified(file.lastModified());
		}
	}
	
	public static synchronized void copyFileToFile(File srcFile, File destFile, 
			boolean replaceIfExists, boolean preserveFileDate) throws IOException {
		if(srcFile.isDirectory() || destFile.isDirectory()) {
			throw new IOException("Os parâmetros do tipo File não podem ser diretórios.");
		}
		if(!deleteIfExists(replaceIfExists, destFile)) {
			return;
		}
		destFile.createNewFile();
		writeBytes(destFile, readBytes(srcFile));
		if(preserveFileDate) {
			destFile.setLastModified(srcFile.lastModified());
		}
	}
	
	public static synchronized void cutDirectoryToDirectory(File srcDir, File destDir, 
			boolean replaceIfExists, boolean preserveFilesDate) throws IOException {
		copyDirectoryToDirectory(srcDir, destDir, replaceIfExists, preserveFilesDate);
		File newDir = new File(destDir, srcDir.getName());
		if(!replaceIfExists && newDir.exists()) {
			return;
		}
		srcDir.delete();
	}
	
	public static synchronized void cutFileToDirectory(File file, File directory, 
			boolean replaceIfExists, boolean preserveFileDate) throws IOException {
		copyFileToDirectory(file, directory, replaceIfExists, preserveFileDate);
		File newFile = new File(directory, file.getName());
		if(!replaceIfExists && newFile.exists()) {
			return;
		}
		file.delete();
	}
	
	public static synchronized void cutFileToFile(File srcFile, File destFile, 
			boolean replaceIfExists, boolean preserveFileDate) throws IOException {
		copyFileToFile(srcFile, destFile, replaceIfExists, preserveFileDate);
		if(!replaceIfExists && destFile.exists()) {
			return;
		}
		srcFile.delete();
	}
	
	public static synchronized void writeBytes(File file, byte[] bytes) throws IOException {
		assertExistence(file, false);
		try(FileOutputStream outputStream = new FileOutputStream(file)) {
			outputStream.write(bytes);
		}
	}
	
	public static synchronized byte[] readBytes(File file) throws IOException {
		if(!file.exists()) {
			return null;
		}
		long length = file.length();
		if(length > Integer.MAX_VALUE) {
			throw new IOException("O arquivo é muito grande.");
		}
		try(FileInputStream inputStream = new FileInputStream(file)) {
			byte[] array = new byte[(int) length];
			inputStream.read(array);
			return array;
		}
	}
	
	public static void renameFile(File file, String newName) {
		String parent = file.getParent();
		file.renameTo(new File((parent == null ? "" : parent) + "\\" + newName));
	}
	
	public static void setHidden(File file, boolean hidden) throws IOException {
		Files.setAttribute(file.toPath(), "dos:hidden", hidden);
	}
	
	public static boolean assertExistence(File file) throws IOException {
		return assertExistence(file, false);
	}
	
	public static boolean assertExistence(File file, boolean folder) throws IOException {
		boolean exists = file.exists();
		if(!exists) {
			if(folder) {
				file.mkdirs();
			} else {
				file.createNewFile();
			}
			exists = true;
		}
		return exists;
	}
	
	private static boolean deleteIfExists(boolean deleteIfExists, File file) {
		if(!deleteIfExists && file.exists()) {
			return false;
		} else {
			if(file.exists()) {
				file.delete();
			}
			return true;
		}
	}
	
	private static String toTextLines(List<String> lines) {
		StringBuilder sb = new StringBuilder();
		int size = lines.size();
		for(int i = 0; i < size; i++) {
			sb.append(lines.get(i) + (i == size - 1 ? "" : "\n"));
		}
		return sb.toString();
	}
	
}
