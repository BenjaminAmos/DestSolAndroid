import com.android.build.api.transform.Transform
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.Format

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassReader


class RedirectClassesTransform extends Transform {
    private List<String> packages;

    public RedirectClassesTransform(List<String> packages) {
        this.packages = packages;
    }

    @Override
    public java.lang.String getName() {
        return 'RedirectClassesTransform';
    }

    @Override
    public java.util.Set<QualifiedContent.ContentType> getInputTypes() {
        return Collections.singleton(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    public java.util.Set<? super QualifiedContent.Scope> getScopes() {
        return EnumSet.of(QualifiedContent.Scope.EXTERNAL_LIBRARIES, QualifiedContent.Scope.PROJECT);
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws TransformException, java.lang.InterruptedException, java.io.IOException {
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        for (TransformInput input : inputs) {
            Path outDir = null;
            Path rootDir = null;
            boolean copyFiles = true;
            FileVisitor<Path> fileVisitor = new FileVisitor<Path>() {
                @Override
                FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    return FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    if (path.toString().endsWith(".class")) {
                        Path newPath = path;
                        if (copyFiles) {
                            newPath = outDir.resolve(rootDir.relativize(path));
                            newPath.toFile().mkdirs()
                            Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
                        }

                        if (!packages.stream().anyMatch({element -> path.startsWith("/" + element.replace(".", "/"))})
                            || path.toString().contains("/org/destinationsol/android/compat")) {
                            // Some of the compatibility classes reference the ones that they are supposed to replace,
                            // so they should not be modified.
                            return FileVisitResult.CONTINUE
                        }

                        try {
                            byte[] classBytes = Files.readAllBytes(newPath);
                            ClassReader reader = new ClassReader(classBytes);
                            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)
                            reader.accept(new RedirectingClassVisitor(Opcodes.ASM7, writer), ClassReader.EXPAND_FRAMES)
                            byte[] newClassBytes = writer.toByteArray()
                            Files.write(path, newClassBytes, StandardOpenOption.TRUNCATE_EXISTING)
                        } catch(Throwable t) {
                            println "Error processing ${path.toString()}: ${t.toString()}"
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE
                }
            };

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                rootDir = directoryInput.getFile().toPath();
                outDir = transformInvocation.getOutputProvider().getContentLocation(directoryInput.name, getInputTypes(), getScopes(), Format.DIRECTORY).toPath();
                try {
                    Files.walkFileTree(rootDir, fileVisitor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (JarInput jarInput : input.getJarInputs()) {
                File jarFile = jarInput.getFile();
                File outJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, getInputTypes(), getScopes(), Format.JAR);
                Files.copy(jarFile.toPath(), outJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                FileSystem jarFileSystem
                try {
                    jarFileSystem = FileSystems.newFileSystem(URI.create("jar:" + outJar.toURI()), new HashMap<String, Object>())
                    copyFiles = false
                    for (Path jarDir : jarFileSystem.getRootDirectories()) {
                        Files.walkFileTree(jarDir, fileVisitor);
                    }
                } catch (Exception e) {
                    e.printStackTrace()
                } finally {
                    if (jarFileSystem != null) {
                        jarFileSystem.close()
                    }
                }
            }
        }
    }
}