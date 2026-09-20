"""Configuration for the stub Android C++ toolchain in this package.

Modelled on rules_cc's empty_cc_toolchain_config.bzl, which is what rules_cc
itself registers on machines with no C compiler.
"""

load("@rules_cc//cc/common:cc_common.bzl", "cc_common")
load("@rules_cc//cc/toolchains:cc_toolchain_config_info.bzl", "CcToolchainConfigInfo")

def _impl(ctx):
    return cc_common.create_cc_toolchain_config_info(
        ctx = ctx,
        toolchain_identifier = "android_stub",
        host_system_name = "local",
        target_system_name = "android",
        target_cpu = "android",
        target_libc = "local",
        compiler = "none",
        abi_version = "local",
        abi_libc_version = "local",
    )

cc_toolchain_config = rule(
    implementation = _impl,
    provides = [CcToolchainConfigInfo],
)
