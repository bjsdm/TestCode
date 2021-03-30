package bjsdm.plugin

import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project


public class ClickPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        println("配置成功--------->ClickPlugin")
        project.android.registerTransform(new ClickTransform())
    }
}