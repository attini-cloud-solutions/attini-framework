package attini.deploy.origin.zip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    public static Path unzip(byte[] bytes) {
        try {
            Path zip_destination = Files.createTempDirectory("");

            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(zip_destination.toFile(), zipEntry);
                if (zipEntry.getName().trim().endsWith("/")) {
                    Files.createDirectory(newFile.toPath());
                } else {
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer, 0, buffer.length)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }

                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            return zip_destination;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
