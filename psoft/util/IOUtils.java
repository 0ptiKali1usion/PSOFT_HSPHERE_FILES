package psoft.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import psoft.hsp.tools.PkgBuilder;

/* loaded from: hsphere.zip:psoft/util/IOUtils.class */
public class IOUtils {
    static final int COPY_BUFFER_SIZE = 8096;
    static final AcceptAllFilter acceptAll = new AcceptAllFilter();

    /* loaded from: hsphere.zip:psoft/util/IOUtils$AcceptAllFilter.class */
    static class AcceptAllFilter implements FilenameFilter {
        AcceptAllFilter() {
        }

        @Override // java.io.FilenameFilter
        public boolean accept(File file, String s) {
            return true;
        }
    }

    public static void mkdir(String dirname) throws IOException {
        if (dirname == null || "".equals(dirname)) {
            throw new IOException("Directory to create is not specified.");
        }
        if (!dirname.startsWith(PkgBuilder.PKG_PREFIX) && !dirname.startsWith("/")) {
            dirname = PkgBuilder.PKG_PREFIX + dirname;
        }
        File path = new File(dirname);
        if (!path.isDirectory()) {
            path.mkdirs();
        }
    }

    public static void rmdir(File f) throws IOException {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File file : files) {
                rmdir(file);
            }
        }
        f.delete();
    }

    public static void jarDirectory(File dirToJar, File jarFile, FilenameFilter filenameFilter) throws IOException {
        jarDirectory(dirToJar, new FileOutputStream(jarFile), filenameFilter, jarFile);
    }

    public static void jarDirectory(File file, OutputStream outputstream, FilenameFilter filenamefilter, File exclude) throws IOException {
        JarOutputStream jaroutputstream = new JarOutputStream(new BufferedOutputStream(outputstream));
        ArrayList arraylist = new ArrayList();
        if (file.isDirectory()) {
            listDir(arraylist, file, "", filenamefilter);
        }
        Iterator iterator = arraylist.iterator();
        while (iterator.hasNext()) {
            String s = (String) iterator.next();
            File newFile = new File(file, s);
            if (!newFile.equals(exclude)) {
                ZipEntry zipentry = new ZipEntry(s);
                jaroutputstream.putNextEntry(zipentry);
                FileInputStream fileinputstream = new FileInputStream(newFile);
                copy(fileinputstream, jaroutputstream);
                fileinputstream.close();
            }
        }
        jaroutputstream.closeEntry();
        jaroutputstream.close();
    }

    private static void listDir(List list, File dir, String path, FilenameFilter filenamefilter) {
        String[] as = dir.list(filenamefilter);
        for (String s1 : as) {
            File file1 = new File(dir, s1);
            if (file1.isDirectory()) {
                listDir(list, file1, path + s1 + "/", filenamefilter);
            } else {
                list.add(path + s1);
            }
        }
    }

    public static void copy(InputStream is, OutputStream out) throws IOException {
        byte[] buf = new byte[COPY_BUFFER_SIZE];
        while (true) {
            int count = is.read(buf);
            if (count > -1) {
                out.write(buf, 0, count);
            } else {
                return;
            }
        }
    }

    public static void recursiveCopy(File src, File dst, FilenameFilter filter) throws IOException {
        if (filter.accept(src.getParentFile(), src.getName())) {
            if (src.isDirectory()) {
                dst.mkdirs();
                copyDir(src, dst, filter);
            } else if (dst.exists() && dst.isDirectory()) {
                copyFile(src, new File(dst, src.getName()));
            } else {
                copyFile(src, dst);
            }
        }
    }

    public static void recursiveCopy(File src, File dst) throws IOException {
        recursiveCopy(src, dst, acceptAll);
    }

    public static void copyDir(File src, File dst) throws IOException {
        copyDir(src, dst, acceptAll);
    }

    public static void copyDir(File src, File dst, FilenameFilter filter) throws IOException {
        File[] afile = src.listFiles();
        for (File file2 : afile) {
            if (filter.accept(src, file2.getName())) {
                File file3 = new File(dst, file2.getName());
                if (file2.isDirectory()) {
                    file3.mkdir();
                    copyDir(file2, file3, filter);
                } else {
                    copyFile(file2, file3);
                }
            }
        }
    }

    public static void copyDirRecursively(File src, File dst, boolean overwrite) throws IOException {
        if (!src.isDirectory()) {
            throw new IOException("'" + src.getAbsolutePath() + "' is not a directory.");
        }
        if (!dst.isDirectory() && !dst.mkdirs()) {
            throw new IOException("Unable to create the '" + dst.getAbsolutePath() + "' output directory.");
        }
        String[] content = src.list();
        for (String name : content) {
            if (name != null) {
                File srcItem = new File(src, name);
                File dstItem = new File(dst, name);
                if (srcItem.isDirectory()) {
                    copyDirRecursively(srcItem, dstItem, overwrite);
                } else if (srcItem.isFile()) {
                    copyFile(srcItem, dstItem, overwrite);
                } else {
                    throw new IOException("Unable to find '" + srcItem.getAbsolutePath() + "'.");
                }
            }
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        if (!src.isFile()) {
            throw new IOException("'" + src.getAbsolutePath() + "' is not an existing source file.");
        }
        File dstDirectory = dst.getParentFile();
        if (!dstDirectory.isDirectory() && !dstDirectory.mkdirs()) {
            throw new IOException("Unable to create the '" + dstDirectory.getAbsolutePath() + "' output directory.");
        }
        FileInputStream fileinputstream = new FileInputStream(src);
        FileOutputStream fileoutputstream = new FileOutputStream(dst);
        copy(fileinputstream, fileoutputstream);
        fileinputstream.close();
        fileoutputstream.close();
    }

    public static void appendFile(File src, File dst) throws IOException {
        if (!src.isFile()) {
            throw new IOException("'" + src.getAbsolutePath() + "' is not an existing source file.");
        }
        File dstDirectory = dst.getParentFile();
        if (!dstDirectory.isDirectory() && !dstDirectory.mkdirs()) {
            throw new IOException("Unable to create the '" + dstDirectory.getAbsolutePath() + "' output directory.");
        }
        FileInputStream fileinputstream = new FileInputStream(src);
        FileOutputStream fileoutputstream = new FileOutputStream(dst.getAbsolutePath(), true);
        copy(fileinputstream, fileoutputstream);
        fileinputstream.close();
        fileoutputstream.close();
    }

    protected static void copyStream2File(InputStream is, File dst) throws IOException {
        File dstDirectory = dst.getParentFile();
        if (!dstDirectory.isDirectory() && !dstDirectory.mkdirs()) {
            throw new IOException("Unable to create the '" + dstDirectory.getAbsolutePath() + "' output directory.");
        }
        FileOutputStream fileoutputstream = new FileOutputStream(dst);
        copy(is, fileoutputstream);
        fileoutputstream.close();
    }

    public static void copyFile(File src, File dst, boolean overwrite) throws IOException {
        if (dst.isFile()) {
            if (overwrite) {
                if (!dst.delete()) {
                    throw new IOException("Unable to delete the '" + dst.getAbsolutePath() + "' existing file");
                }
                dst.createNewFile();
            } else {
                throw new IOException("'" + dst.getAbsolutePath() + "' already exists.");
            }
        }
        copyFile(src, dst);
    }

    public static void copyFile(String srcName, String dstName, boolean overwrite) throws IOException {
        if (srcName == null || dstName == null) {
            throw new IOException("Unable to copy '" + srcName + "' into '" + dstName + "'.");
        }
        File inputFile = new File(srcName);
        File outputFile = new File(dstName);
        copyFile(inputFile, outputFile, overwrite);
    }

    public static void copyDirRecursively(String sourcePath, String dstPath, boolean overwrite) throws IOException {
        if (sourcePath == null || dstPath == null) {
            throw new IOException("Unable to copy '" + sourcePath + "' into '" + dstPath + "'.");
        }
        File srcDir = new File(sourcePath);
        File dstDir = new File(dstPath);
        copyDirRecursively(srcDir, dstDir, overwrite);
    }

    public static void unpackJar(File jarArchive, File dstDirectory) throws IOException {
        if (!jarArchive.isFile()) {
            throw new IOException("'" + jarArchive.getAbsolutePath() + "' is not an existing archive file file.");
        }
        if (!dstDirectory.isDirectory() && !dstDirectory.mkdirs()) {
            throw new IOException("Unable to create the '" + dstDirectory.getAbsolutePath() + "' directory to extract the archive into it.");
        }
        JarFile jarFile = new JarFile(jarArchive);
        Enumeration e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry zipentry = e.nextElement();
            if (zipentry.isDirectory()) {
                new File(dstDirectory, zipentry.getName()).mkdirs();
            } else {
                copyStream2File(jarFile.getInputStream(zipentry), new File(dstDirectory, zipentry.getName()));
            }
        }
    }

    public static File makeTempDirectory(File baseDirectory) throws IOException {
        if (!baseDirectory.isDirectory()) {
            baseDirectory.mkdirs();
        }
        File tmpFile = File.createTempFile("hsp_", null, baseDirectory);
        String tmpName = tmpFile.getName();
        tmpFile.delete();
        File tmpFile2 = new File(baseDirectory, tmpName);
        tmpFile2.mkdir();
        return tmpFile2;
    }

    public static boolean removeFilesByMask(String path, final String ext) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        File[] filesToRemove = dir.listFiles(new FileFilter() { // from class: psoft.util.IOUtils.1
            @Override // java.io.FileFilter
            public boolean accept(File file) {
                if (file.getName().endsWith(ext) && !file.isDirectory()) {
                    return true;
                }
                return false;
            }
        });
        boolean deletedSucessfully = false;
        for (int i = 0; i < filesToRemove.length; i++) {
            if (filesToRemove[i].exists()) {
                deletedSucessfully = deletedSucessfully || filesToRemove[i].delete();
            }
        }
        return true;
    }
}
