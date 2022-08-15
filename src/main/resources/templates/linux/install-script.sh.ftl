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

CONTENT=content
BASE_DIR=$HOME/.local
APP_NAME=${appSafeName}
TARGET_DIR=$BASE_DIR/AppForge/$APP_NAME
SHARE_DIR=$BASE_DIR/share/applications
mkdir -p $TARGET_DIR

cp -rv $CONTENT/* $TARGET_DIR
# process .desktop file
sed "s|__HOME_DIR__|$HOME|g" $CONTENT/$APP_NAME.desktop > $SHARE_DIR/$APP_NAME.desktop
