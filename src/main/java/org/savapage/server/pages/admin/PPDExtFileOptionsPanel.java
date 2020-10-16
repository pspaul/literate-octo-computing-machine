/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.admin;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PPDExtFileOptionsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            Wicket ID.
     */
    public PPDExtFileOptionsPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param fontFamilyDefault
     */
    public void populate() {

        final List<String> ppdeList = new ArrayList<>();
        ppdeList.add("");

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path path,
                    final BasicFileAttributes attrs) throws IOException {
                final File file = path.toFile();
                if (file.isFile() && !file.isHidden() && !FilenameUtils
                        .isExtension(file.getName(), "template")) {
                    ppdeList.add(file.getName());
                }
                return CONTINUE;
            }
        };

        final Set<FileVisitOption> options = new HashSet<>();
        try {
            Files.walkFileTree(Paths.get(
                    ConfigManager.getServerCustomCupsHome().getAbsolutePath()),
                    options, 1, visitor);
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }

        java.util.Collections.sort(ppdeList, String.CASE_INSENSITIVE_ORDER);

        add(new PropertyListView<String>("panel-ppde-ext", ppdeList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {

                final String fileName = item.getModel().getObject();

                final Label label = new Label("option-ppde-ext", fileName);
                label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                        fileName));
                item.add(label);
            }
        });
    }

}
