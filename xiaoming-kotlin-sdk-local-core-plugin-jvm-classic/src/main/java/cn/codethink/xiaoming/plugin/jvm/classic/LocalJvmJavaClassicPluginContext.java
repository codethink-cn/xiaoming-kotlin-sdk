/*
 * Copyright 2024 CodeThink Technologies and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.codethink.xiaoming.plugin.jvm.classic;

import cn.codethink.xiaoming.common.Cause;
import cn.codethink.xiaoming.plugin.PluginKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LocalJvmJavaClassicPluginContext implements LocalJvmClassicPluginContext {
    @NotNull
    final LocalJvmClassicPlugin plugin;

    @NotNull
    final Cause cause;

    @NotNull
    final Logger logger;

    @Nullable
    Cause error;

    public LocalJvmJavaClassicPluginContext(
            @NotNull LocalJvmClassicPlugin plugin,
            @NotNull Cause cause
    ) {
        this.plugin = plugin;
        this.cause = cause;
        this.logger = LoggerFactory.getLogger(
                Objects.requireNonNullElseGet(
                        plugin.getMeta().getLogger(),
                        () -> PluginKt.getId(plugin).toString()
                )
        );
    }

    @NotNull
    @Override
    public LocalJvmClassicPlugin getPlugin() {
        return plugin;
    }

    @NotNull
    @Override
    public Cause getCause() {
        return cause;
    }

    @NotNull
    public Logger getLogger() {
        return logger;
    }

    @Nullable
    @Override
    public Cause getError() {
        return error;
    }

    public void setError(@Nullable Cause error) {
        this.error = error;
    }
}
