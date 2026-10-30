set pagination off
set confirm off
set debuginfod enabled off
set print thread-events off
target remote 127.0.0.1:31360
source research/dkii-lava/trace_bump_setting.py
continue
detach
quit
