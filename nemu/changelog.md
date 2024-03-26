# major changes
## 2024/03/26 iringbuf
我们选择:当`ITRACE`开启时，总是记录并在`assert_fail_msg`中打印iringbuf。

因为itrace不包含未执行完的指令，所以原先的itrace设施发生了一些改变.

### 1. 在`cpu-exec.c`中：
`exec_once`中的itrace功能移动到`itrace_generate`中.

`itrace_generate`会:1.生成itrace 2.将itrace加入iringbuf

### 2. 在`inst.c`中:
在`isa_exec_once`中，`inst_fetch`完后我们就会进行`itrace_generate`，以保证我们会记录到可能触发assert fail的指令。

## 2024/03/26 mtrace
Kconfig加入mtrace选项
现在mtrace默认会和itrace一起输出到log中