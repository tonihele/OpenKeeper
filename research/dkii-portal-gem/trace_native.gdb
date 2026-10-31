set pagination off
set confirm off
set debuginfod enabled off
set breakpoint pending on
set logging file /tmp/dkii-gem-gdb.log
set logging overwrite on
set logging enabled on
target remote 127.0.0.1:31357
source research/dkii-portal-gem/trace_native.py
continue
detach
quit
