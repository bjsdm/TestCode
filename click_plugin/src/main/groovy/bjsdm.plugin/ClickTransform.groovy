package bjsdm.plugin

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.bjsdm.click_plugin.ClickClassVisitor
import org.gradle.internal.file.FileType
import org.gradle.internal.impldep.org.apache.ivy.util.FileUtil
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.nio.file.spi.FileTypeDetector
import java.util.jar.JarFile


public class ClickTransform extends Transform {

    @Override
    String getName() {
        return "ClickTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        //获取输入项进行遍历
        def transformInputs = transformInvocation.inputs
        //获取输出目录
        def transformOutputProvider = transformInvocation.outputProvider
        transformInputs.each { TransformInput transformInput ->
            //遍历 jar 包
            transformInput.jarInputs.each { JarInput jarInput ->
                //直接将 jar 包 copy 到输出目录
                File dest = transformOutputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }
            //遍历目录
            transformInput.directoryInputs.each { DirectoryInput directoryInput ->
                //获取目录里面的 class 文件
                directoryInput.file.eachFileRecurse { File file ->
                    if (file.absolutePath.endsWith(".class")){
                        //对于class文件进行读取解析
                        def classReader = new ClassReader(file.bytes)
                        //将class文件内容写入到ClassWriter中
                        def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                        //使用ClickClassVisitor去读取内容
                        def classVisitor = new ClickClassVisitor(classWriter)
                        //开始读取
                        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                        //获取修改后的内容
                        def bytes = classWriter.toByteArray()
                        //覆盖之前的文件
                        def outputStream = new FileOutputStream(file.path)
                        outputStream.write(bytes)
                        outputStream.close()
                    }
                }
                //将 Directory 的文件 copy 到输出目录
                File dest = transformOutputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
    }

}