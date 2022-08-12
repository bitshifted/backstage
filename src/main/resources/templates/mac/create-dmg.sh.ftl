#
# /*
#  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
#  *
#  * This Source Code Form is subject to the terms of the Mozilla Public
#  * License, v. 2.0. If a copy of the MPL was not distributed with this
#  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
#  */
#

#! /bin/bash
INSTALLER_DIR=${installerDir}
MOUNT_DIR=$INSTALLER_DIR/mount
RAW_DMG_NAME=${appSafeName}-raw.dmg
FINAL_DMG_NAME=${appSafeName}-${appVersion}-mac.dmg
DMG_SIZE=${sizeInMb}
VOLUME_NAME="${appName}"
BACKGROUND_IMAGE=$INSTALLER_DIR/appforge-background.png
APP_BUNDLE_NAME="${appName}"
APP_BUNDLE_DIR="${macOutputDir}"
DS_STORE_FILE=$INSTALLER_DIR/DS_Store

cd $INSTALLER_DIR
dd if=/dev/zero of=$RAW_DMG_NAME bs=1M count=$DMG_SIZE status=progress
mkfs.hfsplus -v "$VOLUME_NAME" $RAW_DMG_NAME

# mount the image
sudo mkdir $MOUNT_DIR
sudo mount -o loop $RAW_DMG_NAME $MOUNT_DIR

# create content
# background image
sudo mkdir $MOUNT_DIR/.background
sudo cp $BACKGROUND_IMAGE $MOUNT_DIR/.background
# create Applications link
cd $MOUNT_DIR
sudo ln -s /Applications Applications
# copy application bundle
sudo mkdir $APP_BUNDLE_NAME.app
sudo cp -rv $APP_BUNDLE_DIR/* $APP_BUNDLE_NAME.app

# copy .DS_Store file
sudo cp $DS_STORE_FILE $MOUNT_DIR/.DS_Store

# cleanup
cd $INSTALLER_DIR
sudo umount $MOUNT_DIR

# compress raw DMG image
dmg $RAW_DMG_NAME $FINAL_DMG_NAME

# cleanup
rm $RAW_DMG_NAME $DS_STORE_FILE $BACKGROUND_IMAGE
rm -rvf mount




