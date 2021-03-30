原本是想写一篇介绍字节码插桩的文章，但无奈的是使用字节码插桩之前需要使用到自定义 Gradle Plugin，似乎暗示着这篇文章并不会短。

<img src="https://img-blog.csdnimg.cn/20210328193903413.png" width = "150" >


---

在了解字节码插桩之前，我们先了解编译插桩。

## 编译插桩是什么

相信大家都使用过 ButterKnife，了解过它原理的都知道，它是在编译期间生成相应的 java 文件，到运行时，通过反射机制去获取该生成类，并调用其绑定方法，从而做到控件绑定。（什么？你还没了解过 ButterKnife 原理？赶紧去看看吧——<a href="https://juejin.cn/post/6943829469106798629">《从手写ButterKnife到掌握注解、AnnotationProcessor》</a>）

**编译插桩其实就是指在代码编译期间修改已有的代码或者生成新代码。**

## 字节码插桩是什么
字节码插桩其实就是比编译插桩更细化一步，编译插桩的范围是指编译过程中，这里包含了`java --> class --> dex`的整套流程，而**字节码插桩只针对于 class 这一步，对生成后的 class 文件进行修改**。

## 字节码插桩前言

首先，我们先了解下什么情况下会用到字节码插桩。**学技术并不是为了秀技术，而是为了解决业务问题。**

我们先想象一个业务场景——我们需要记录用户的点击事件，这时，我们会怎么做？

- 在每个 onClick() 方法下调用统计代码？这也太繁琐了！更何况人总会有忘记的时候，很容易出现遗漏的情况。
- 创建新的点击类，每次设置点击监听时使用新的点击类？在自己写的代码上用还好，但是第三方库类怎么办？

这时就可以用上字节码插桩了！因为 Java 文件编译成 class 后，这时可以获取全部的 class 文件，包含自己写的代码和其它库类的。拿到 class 文件后，就可以进行批量修改，并且对于 Java 文件是无感的，因为我们只针对 class 文件。

在使用字节码插桩之前，我们需要获取到每个 class 文件，这时，需要使用到自定义Transform，而自定义Transform 需要在自定义 Gradle Plugin 时进行注册，所以，我们需要先学习下如何自定义一个 Gradle Plugin。

<img src="https://img-blog.csdnimg.cn/20210326220150376.jpg" width = "150" >
 
 说明文本终于写完，赶紧系好安全带，准备出发，Go！Go！Go！

## 自定义Gradle

> 由于这是我在 Android Studio 里面进行创建自定义 Gradle，所以很多配置需要自己手写，会比较麻烦，不过没关系，我都写好了，到时你们进行 copy 即可。

### 创建Module

首先，我们需要新建一个 Module：

Android Studio --> File --> New --> New Module --> Java or Kotlin Library --> click_plugin（命名自取）

<img src="https://img-blog.csdnimg.cn/20210328210534885.png" width = "250" >

### 更新build.gradle

覆盖掉原有的`build.gradle`文件内容：

```
apply plugin: 'groovy'
apply plugin: 'maven'
dependencies {
    implementation fileTree(dir: 'libs', includes: ['*.jar'])
    implementation gradleApi()
    implementation localGroovy()
    implementation 'com.android.tools.build:gradle:4.0.0'
}
//这两个是配置信息，后续会用到，命名自取
group='bjsdm.plugin'
version='1.0.0'
uploadArchives{
    repositories{
        mavenDeployer{
            // 本地的 Maven 地址设置
            // 部署到本地，也就是项目的根目录下
            // 部署成功会创建一个 bjsdm_repo 文件夹，命名自取
            repository(url: uri('../bjsdm_repo'))
        }
    }
}
```

主要注意的是这三个配置：

- `group='bjsdm.plugin'`
- `version='1.0.0'`
- `repository(url: uri('../bjsdm_repo'))`


### 创建 Plugin

创建`ClickPlugin.groovy`文件

<img src="https://img-blog.csdnimg.cn/20210329011925705.png" width = "650" >

这里的包名报错不用管它，识别出了问题。


```
package bjsdm.plugin
import org.gradle.api.Plugin
import org.gradle.api.Project
public class ClickPlugin implements Plugin<Project>{
    @Override
    void apply(Project project) {
        println("配置成功--------->ClickPlugin")
    }
}
```

### 配置

进行配置：

<img src="https://img-blog.csdnimg.cn/20210329005405310.png" width = "650" >

```
implementation-class=bjsdm.plugin.ClickPlugin
```

这个就是刚刚所写的类，至于`bjsdm.click.properties`的命名可以自取，后续配置也会用到。

### 部署

进行 plugin 的部署任务：

<img src="https://img-blog.csdnimg.cn/20210329005749685.png" width = "350" >

对`uploadArchives`进行双击操作。

可以看到在根目录下有以下文件生成：

<img src="https://img-blog.csdnimg.cn/20210329010221463.png" width = "350" >

### 依赖

好了，东西生成完毕，这时需要在 app 的`build.gradle`进行依赖即可：

<img src="https://img-blog.csdnimg.cn/20210329010826705.png" width = "450" >

主要的是蓝框里面的配置，也就是所提醒的命名，忘了的话，可以翻回去看看。

```
apply plugin: 'bjsdm.click'
buildscript {
    repositories{
        google()
        jcenter()
        //自定义插件仓库地址
        maven {url '../bjsdm_repo'}
    }
    dependencies {
        //加载自定义插件 group + module + version
        classpath 'bjsdm.plugin:click_plugin:1.0.0'
    }
}
```

### 测试

使用构建命令进行测试：

```
./gradlew clean assembledebug
```

<img src="https://img-blog.csdnimg.cn/20210329012059558.png" width = "550" >

成功输出了！说明我们创建自定义 Gradle Plugin 成功！

<img src="https://img-blog.csdnimg.cn/20210326215055356.jpg" width = "150" >


## 自定义Transform


创建`ClickTransform.groovy`文件：


<img src="https://img-blog.csdnimg.cn/20210329194620782.png" width = "650" >


### 重写方法说明

- `getName()`：设置名字。
- `getInputTypes()`：用于过滤文件类型。填什么类型，就把该类型的全部文件返回。默认有以下两种类型：
	- `QualifiedContent.DefaultContentType.CLASSES`：class 文件类型。
	- `QualifiedContent.DefaultContentType.RESOURCES`：资源文件类型。
- `getScopes()`：用于规定检索的范围：
	- `QualifiedContent.Scope.PROJECT`：主 Project。
	- `QualifiedContent.Scope.SUB_PROJECTS`：其它 Module。
	- `QualifiedContent.Scope.EXTERNAL_LIBRARIES`：外部库。
	- `QualifiedContent.Scope.TESTED_CODE`：当前变量的测试代码，包含依赖库。
	- `QualifiedContent.Scope.PROVIDED_ONLY`：本地或远程的依赖项。
	- `QualifiedContent.Scope.PROJECT_LOCAL_DEPS`：主 Project 的本地依赖项，包含本地 jar，**已废弃**，使用``QualifiedContent.Scope.EXTERNAL_LIBRARIES``代替
	- `QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS`：其它 Module 的本地依赖项，包含本地 jar，**已废弃**，使用``QualifiedContent.Scope.EXTERNAL_LIBRARIES``代替
- `isIncremental()`：是否支持增量编译。

根据上述内容，我们可以完善下`ClickTransform.groovy`：

```
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
}
```

其中：

- `TransformManager.CONTENT_CLASS`：ImmutableSet.of(CLASSES)，其实就是把`QualifiedContent.DefaultContentType.CLASSES`放到 Set 中。
- `TransformManager.SCOPE_FULL_PROJECT`：ImmutableSet.of(Scope.PROJECT, Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES)，含义同上。

以上两个参数都可以根据不同场景自己进行取值。

<img src="https://img-blog.csdnimg.cn/20210329222955824.png" width = "120" >

好像忘了什么，不是说，全部 class 文件都可以经过 Transform 吗？不然怎么做字节码插桩操作。

### transform()


确实，所以，我们还需要重写`Transform`的`transform()`方法：（旧版本）

```
    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
    }
```

不过，以上方法已被弃用，现在推荐使用：（新版本）

```
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
    }
```

这两个版本的区别在于，把旧版本中的全部参数封装到新版本的`TransformInvocation`中，但其实新版本还是在默认调用旧版本的方法：

```
    public void transform(@NonNull TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        // Just delegate to old method, for code that uses the old API.
        //noinspection deprecation
        transform(transformInvocation.getContext(), transformInvocation.getInputs(),
                transformInvocation.getReferencedInputs(),
                transformInvocation.getOutputProvider(),
                transformInvocation.isIncremental());
    }
```


`transform()`方法就像加工通道，我们通过`inputs`把东西放进去，加工完后，就放到`outputProvider`中。

- `inputs`：传过来的数据，有两种格式：
	- jar 包格式。以 jar 包的形式参与编译，例如依赖的 jar 包。
	- 目录格式。以源码的形式参与编译，例如我们在项目中书写的代码。
- `outputProvider`：输出的目录，将修改完的文件复制到输出目录中。

**一定要重写`transform()`**，因为`Transform`里面的`transform()`方法是个空方法：

```
    @Deprecated
    @SuppressWarnings("UnusedParameters")
    public void transform(
            @NonNull Context context,
            @NonNull Collection<TransformInput> inputs,
            @NonNull Collection<TransformInput> referencedInputs,
            @Nullable TransformOutputProvider outputProvider,
            boolean isIncremental) throws IOException, TransformException, InterruptedException {
    }
```

相当于在`transform()`这个加工通道中，把东西放进去了，但是没东西出来一样，因为没有把修改完后的文件复制到输出目录中。


所以，我们现在先实现一个基础功能，打印所放进去的东西的名字：

```
    @Override
    void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        //获取输入项进行遍历
        def transformInputs = transformInvocation.inputs
        transformInputs.each { TransformInput transformInput ->
            //遍历 jar 包
            transformInput.jarInputs.each { JarInput jarInput ->
                println("jarInput：" + jarInput)
                //使用 JarFile 进行解压
                def enumeration = new JarFile(jarInput.file).entries()
                while (enumeration.hasMoreElements()){
                    //获取 jar 里面的内容
                    def entry = enumeration.nextElement()
                    println("jarInput File：" + entry.name)
                }
            }
            //遍历目录
            transformInput.directoryInputs.each { DirectoryInput directoryInput ->
                println("directoryInputs：" + directoryInput)
                //获取目录里面的文件
                directoryInput.file.eachFileRecurse { File file ->
                    println("directoryInputs File：" + file.name)
                }
            }
        }
    }
```

### 注册

我们将上面写好的 Transform 注册到 ClickPlugin 中：

```
    @Override
    void apply(Project project) {
        println("配置成功--------->ClickPlugin")
        project.android.registerTransform(new ClickTransform())
    }
```

双击 uploadArchives 部署，运行`./gradlew clean assembledebug`命令：

<img src="https://img-blog.csdnimg.cn/20210330101856627.png" width = "650" >

<img src="https://img-blog.csdnimg.cn/20210330101944467.png" width = "650" >

由于输出的内容太多了，就不全部截取，只截取部分，很明显地有输出：

- jarInput
- jarInput File
- directoryInputs
- directoryInputs File

<img src="https://img-blog.csdnimg.cn/20210326215055356.jpg" width = "150" >

到这里，终于可以松一口气了，基本流程已经走通，只剩下字节码的更改和把修改后的文件放到输出目录。在这里，我只演示更改 directory 下面的 class 文件，至于 jar 包的 class 文件的更改类似，有兴趣再去深究。

## 字节码处理

关于字节码处理，这里使用了 ASM 工具，主要用到其三个类：

- ClassReader：负责对 .class 文件进行读取解析。
- ClassVisitor：负责访问 .class 文件中各个元素，例如读取到方法的时候，会自动调用内部相应的 MethodVisitor。明显的分工操作。
- ClassWriter：生成字节码工具类，将字节码输出为 byte 数组。

我们在编写代码前先在`build.gradle(:click_plugin)`加个依赖：

```
    //ASM依赖
    implementation 'org.ow2.asm:asm:9.1'
    implementation 'org.ow2.asm:asm-commons:9.1'
```


### ClassVisitor

我们需要创建一个 ClassVisitor 对 class 文件进行过滤操作，对于符合条件的 method 修改其相应的 method 读取。

`ClickClassVisitor.java`，存放在：

<img src="https://img-blog.csdnimg.cn/2021033014100491.png" width = "250" >






```
public class ClickClassVisitor extends ClassVisitor {
    public ClickClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM4, classVisitor);
    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions);
        //判断方法
        if (name.startsWith("onClick")) {
            System.out.println("onClick");
            //处理点击方法
            return new ClickMethodVisitor(methodVisitor);
        }
        return methodVisitor;
    }
}

```

### MethodVisitor

在进行代码插入前，我们需要先了解下我们所插入的代码：

```
Log.e("TAG", "CLICK")
```

但是在执行时，其实是分成三部分：

- “TAG”  --> 使用 A 代指
- "CLICK" --> 使用 B 代指
- e(Log, A, B)

为什么要把字符串抽出来呢？

那时因为字符串在字节码结构中，其实也是一张表，一张存在常量池的表：

<img src="https://img-blog.csdnimg.cn/20210327005236121.jpg" width = "250" >

具体的关于字节码结构的介绍，可以参考<a href="https://juejin.cn/post/6944517233674551304">《字节码结构分析》</a>。

至于这段代码真正编译出来的字节码，我们可以通过反编译进行查看。假如是使用 Android Studio 开发，使用 kotlin 语言，可以直接使用`Tools --> Kotlin --> Show Kotlin Bytecode`：

<img src="https://img-blog.csdnimg.cn/20210330144237762.png" width = "650" >

参照以上内容，我们可以继续`ClickMethodVisitor.java`的编写：

```
public class ClickMethodVisitor extends MethodVisitor {
    public ClickMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor);
    }
    @Override
    public void visitCode() {
        super.visitCode();
        mv.visitLdcInsn("TAG");
        mv.visitLdcInsn("CLICK");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e",
                "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);
    }
}
```

当然，有更简单的方式，安装<a href="https://plugins.jetbrains.com/plugin/14860-asm-bytecode-viewer-support-kotlin">ASM Bytecode Viewer Support Kotlin</a>插件，可以在 Android Studio 里面搜索安装。

安装重启 Android Studio，右键文件 --> Asm Bytecode Viewer ：

<img src="https://img-blog.csdnimg.cn/20210330150122944.png" width = "650" >

修改 transform() 方法：

```
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
```

代码看着多，其实都是注释和 copy 代码，不难理解。

到这里差不多算结束了，不过别忘了写个点击监听事件：

```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_click.setOnClickListener {
            Log.e("TAG", "正常点击事件")
        }
    }
}
```

双击 uploadArchives 部署。

运行...

<img src="https://img-blog.csdnimg.cn/2021033017020682.png" width = "650" >

终于完成...

<img src="https://img-blog.csdnimg.cn/20210328193903413.png" width = "150" >

GitHub 地址：<a href="https://github.com/bjsdm/TestCode">https://github.com/bjsdm/TestCode</a>


---

这是我的公众号，关注获取第一信息！！欢迎关注支持下，谢谢！

<img src="https://img-blog.csdnimg.cn/20210328021432830.png" width = "500" >

















































