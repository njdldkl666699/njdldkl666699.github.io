---
title: Linux内核快速编译
date: 2026-01-17 17:02:52
tags: [Linux, 内核, 编译]
categories: [Linux]
cover: images/高雅Linux.webp
---

本文介绍在Ubuntu 24.04.3 LTS环境下，如何尽可能快速地编译Linux 6.16.12内核的完整流程，包括依赖安装、配置精简、错误修正及编译安装步骤。注意，此方式编译内核仅供学习和测试使用，生产环境请谨慎评估。

## 环境准备

安装构建工具、编译器缓存工具。关键步骤是安装`ccache`，它可以显著加快后续的编译速度。

```bash
sudo apt update
sudo apt upgrade
sudo apt install build-essential libncurses-dev bison flex libssl-dev libelf-dev ccache gawk
```

从[kernel.org](https://www.kernel.org/)下载最新的Linux内核源码包，并解压：

```bash
mkdir -p ~/linux-kernel && cd ~/linux-kernel
wget https://cdn.kernel.org/pub/linux/kernel/v6.x/linux-6.16.12.tar.xz
tar -xvf linux-6.16.12.tar.xz
cd linux-6.16.12
```

如果你是AMD的显卡，可能需要使用 dkms 移除 amdgpu 驱动以避免编译冲突：

```bash
sudo dkms status
sudo dkms remove amdgpu/6.12.xx --all   # 替换为实际版本号
```

## 配置内核

### 生成基础配置

建议使用当前系统的内核配置作为基础，然后处理新旧版本差异：

```bash
uname -r # 查看当前内核版本
cp /boot/config-$(uname -r) .config
make olddefconfig   # 处理新旧版本差异，自动选择默认值
```

### 精简配置

精简配置，关闭未使用的驱动选项：

```bash
yes "" | make localmodconfig
```

注意：执行前请连接所有需使用的外设（鼠标、键盘、网卡、USB设备等），以确保相关驱动被保留。

**关闭BTF**

BTF（BPF Type Format）是一种内核调试信息格式，默认开启，它会显著增加编译时间，并且会在编译时占用大量内存，可能会触发OOM，导致编译被杀死，建议关闭。

`make menuconfig`，在 "Kernel hacking" -> "Compile-time checks and compiler options" 中关闭 "Generate BTF info"。（相关选项名：`CONFIG_DEBUG_INFO_BTF`）

### 修正配置问题

Ubuntu此版本下，配置文件中包含了发行版特定的证书路径，会导致签名问题。可以通过以下命令清空相关字段：

```bash
scripts/config --set-str SYSTEM_TRUSTED_KEYS ""
scripts/config --set-str SYSTEM_REVOCATION_KEYS ""
```

或通过`make menuconfig`，在交互式界面中找到 "Cryptographic API" -> "Certificates for signature checking"，清空相关字段。

## 编译内核

启用`ccache`缓存，调用所有CPU核心，仅编译内核镜像和模块。然后安装编译好的模块和内核镜像，grub会自动更新。

```bash
make CC="ccache gcc" -j$(nproc) bzImage modules
sudo make modules_install
sudo make install
```

如果进行了前面的精简配置，那么编译时间相对来说会非常短。在AMD Ryzen 5 5500U（6核12线程）和4G的内存下，大约只需要15-20分钟左右，而在默认配置下，至少需要1小时。

编译完成后，重启系统，会自动使用新内核启动。

```bash
sudo reboot
uname -a   # 验证当前内核版本为6.16.12
```

如果需要回到旧内核，可以在启动时按住Esc键（VMWare虚拟机），进入Grub菜单，选择旧内核启动即可。

## 清理工作

本节我们介绍如何清理编译过程中产生的临时文件和不再需要的内核版本。

### 清理编译产物

编译完成后，可以使用以下命令清理编译过程中产生的临时文件，释放磁盘空间：

```bash
make clean        # 清理大部分编译产物
make mrproper    # 更彻底地清理，包括配置文件
```

### 删除内核

如果不再需要我们编译安装的内核版本，可以通过以下步骤删除：

重启系统，进入Grub菜单，选择旧内核启动。注意，绝对不要在当前运行的内核下删除它自己。

使用以下命令删除不需要的内核版本（以我们自己安装的6.16.12为例）：

```bash
ls -1 /lib/modules/   # 列出所有内核版本

# 假设我们要删除的版本号为：6.16.12-xxx
sudo rm -rf /lib/modules/6.16.12-xxx    # 删除模块目录
sudo rm -f /boot/vmlinuz-6.16.12-xxx       # 删除内核镜像
sudo rm -f /boot/initrd.img-6.16.12-xxx   # 删除initrd镜像
sudo rm -f /boot/System.map-6.16.12-xxx  # 删除System.map文件
sudo rm -f /boot/config-6.16.12-xxx       # 删除配置文件
# 可能还有.old文件，根据实际情况删除
sudo rm -f /boot/vmlinuz-6.16.12-xxx.old
sudo rm -f /boot/System.map-6.16.12-xxx.old
```

更新Grub配置和initramfs：

```bash
sudo update-initramfs -u
sudo update-grub
```

---

关于Prettier的题外话：

写这篇帖子时，VSCode的Prettier插件更新了，修复了中文和英文混排时格式化会在之间添加多余空格的问题。其实我习惯它增加空格了，突然没有空格还有点不习惯（
