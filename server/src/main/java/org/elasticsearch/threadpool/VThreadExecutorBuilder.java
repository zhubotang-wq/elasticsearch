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
import org.elasticsearch.node.Node;
import org.elasticsearch.vThreadpool.VThreadPoolExecutor;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        final String nodeName = Node.NODE_NAME_SETTING.get(settings);
        return new VThreadExecutorSettings(nodeName);
    }

    @Override
    ThreadPool.ExecutorHolder build(VThreadExecutorSettings settings, ThreadContext threadContext) {
        ExecutorService executorService = new VThreadPoolExecutor(
            settings.nodeName + "/" + super.name(),
            Executors.newVirtualThreadPerTaskExecutor(),
            threadContext
        );
        final ThreadPool.Info info = new ThreadPool.Info(
            name(),
            ThreadPool.ThreadPoolType.VIRTUAL,
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            null,
            null
        );

        return new ThreadPool.ExecutorHolder(executorService, info);
    }

    @Override
    String formatInfo(ThreadPool.Info info) {
        return String.format(
            Locale.ROOT,
            "name [%s], core [%d], max [%d], keep alive [%s]",
            info.getName(),
            info.getMin(),
            info.getMax(),
            info.getKeepAlive()
        );
    }

    static class VThreadExecutorSettings extends ExecutorBuilder.ExecutorSettings {
        VThreadExecutorSettings(String nodeName) {
            super(nodeName);
        }
    }

}
