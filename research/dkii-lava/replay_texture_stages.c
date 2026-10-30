/* Execute the original texture-stage selector with a recording COM stand-in.
 * No Windows entry point, graphics driver, real device or display is invoked.
 * The original executable is read only. */
#define main unused_lighting_main
#include "../dkii-light-radius/isolation/native_light_probe.c"
#undef main

static uint32_t fake_vtable[48];
static struct { uint32_t *vtable; } fake_device={fake_vtable};
static const char *case_name;
static int calls;
static uint32_t __attribute__((stdcall)) stage_state(void *device,uint32_t stage,uint32_t type,uint32_t value) {
    if(device!=&fake_device || stage>7 || type>32) { fputs("Unexpected COM arguments\n",stderr); exit(3); }
    printf("{\"case\":\"%s\",\"event\":\"texture_stage\",\"stage\":%u,\"type\":%u,\"value\":%u}\n",case_name,stage,type,value);
    calls++;
    return 0;
}
static uint32_t __attribute__((stdcall)) render_state(void *device,uint32_t type,uint32_t value) {
    if(device!=&fake_device) exit(3);
    printf("{\"case\":\"%s\",\"event\":\"render_state\",\"type\":%u,\"value\":%u}\n",case_name,type,value);
    calls++;
    return 0;
}
static void run(const char *name,uint32_t previous,uint32_t flags,uint32_t units) {
    case_name=name; calls=0;
    *(uint32_t*)0x76c2c4=(uint32_t)&fake_device;
    *(uint32_t*)0x76c294=previous;
    *(uint32_t*)0x76c2ec=units;
    ((void(*)(uint32_t))0x589250)(flags);
    if(*(uint32_t*)0x76c294!=flags) exit(4);
    printf("{\"case\":\"%s\",\"event\":\"summary\",\"previous_flags\":%u,\"flags\":%u,\"previous_units\":%u,\"units\":%u,\"calls\":%d}\n",name,previous,flags,units,*(uint32_t*)0x76c2ec,calls);
}
int main(int argc,char **argv) {
    if(argc!=2) return 2;
    load_image(argv[1]);
    fake_vtable[40]=(uint32_t)stage_state;
    fake_vtable[22]=(uint32_t)render_state;
    run("bump",0,0x8000,1);
    run("bump_luminance",0,0x10000,1);
    run("restore_from_bump",0x8000,0,3);
    run("restore_from_luminance",0x10000,0,3);
    run("bump_cached",0x8000,0x8000,3);
    run("luminance_cached",0x10000,0x10000,3);
    return 0;
}
