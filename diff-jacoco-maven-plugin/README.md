# diff-jacoco-maven-plugin
### 介绍
基于Git的分支或commit之间的文件差异，扫描增量代码的单测覆盖率。

### 使用方法
#### 配置maven插件
```Xml
<plugin>
    <groupId>io.github.yangziwen</groupId>
    <artifactId>diff-jacoco-maven-plugin</artifactId>
    <version>0.0.4</version>
    <executions>
        <execution>
            <id>pre-test</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>post-test</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-diff</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```
#### 执行测试和单测覆盖率统计
```Shell
# 通过为jacoco.diff.oldrev和jacoco.diff.newrev指定commit或分支实现增量覆盖率扫描
# 如果不指定这两个变量，则进行全量覆盖率扫描

mvn test -Djacoco.diff.oldrev=9ac9182 -Djacoco.diff.newrev=148e358
```
