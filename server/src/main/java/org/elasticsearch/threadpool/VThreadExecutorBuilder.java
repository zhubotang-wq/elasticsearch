/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.threadpool;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;

import java.util.List;

public class VThreadExecutorBuilder extends ExecutorBuilder<VThreadExecutorBuilder.VThreadExecutorSettings> {


    public VThreadExecutorBuilder(String name, boolean isSystemThread) {
        super(name, isSystemThread);
    }

    @Override
    public List<Setting<?>> getRegisteredSettings() {
        return List.of();
    }

    @Override
    VThreadExecutorSettings getSettings(Settings settings) {
        return null;
    }

    @Override
    ThreadPool.ExecutorHolder build(VThreadExecutorSettings settings, ThreadContext threadContext) {
        return null;
    }

    @Override
    String formatInfo(ThreadPool.Info info) {
        return "";
    }

    static class VThreadExecutorSettings extends ExecutorBuilder.ExecutorSettings {
        VThreadExecutorSettings(String nodeName) {
            super(nodeName);
        }
    }



}
