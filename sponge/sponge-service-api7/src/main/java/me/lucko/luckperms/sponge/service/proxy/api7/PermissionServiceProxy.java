/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.sponge.service.proxy.api7;

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.ProxiedServiceObject;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class PermissionServiceProxy implements PermissionService, ProxiedServiceObject {
    private final LPPermissionService handle;

    public PermissionServiceProxy(LPPermissionService handle) {
        this.handle = handle;
    }

    @Override
    public @NonNull SubjectCollection getUserSubjects() {
        return this.handle.getUserSubjects().sponge();
    }

    @Override
    public @NonNull SubjectCollection getGroupSubjects() {
        return this.handle.getGroupSubjects().sponge();
    }

    @Override
    public @NonNull Subject getDefaults() {
        return this.handle.getRootDefaults().sponge();
    }

    @Override
    public @NonNull Predicate<String> getIdentifierValidityPredicate() {
        return this.handle.getIdentifierValidityPredicate();
    }

    @Override
    public CompletableFuture<SubjectCollection> loadCollection(@NonNull String s) {
        return CompletableFuture.completedFuture(this.handle.getCollection(s).sponge());
    }

    @Override
    public @NonNull Optional<SubjectCollection> getCollection(String s) {
        return Optional.ofNullable(this.handle.getLoadedCollections().get(s.toLowerCase(Locale.ROOT))).map(LPSubjectCollection::sponge);
    }

    @Override
    public CompletableFuture<Boolean> hasCollection(String s) {
        return CompletableFuture.completedFuture(this.handle.getLoadedCollections().containsKey(s.toLowerCase(Locale.ROOT)));
    }

    @Override
    public @NonNull Map<String, SubjectCollection> getLoadedCollections() {
        return this.handle.getLoadedCollections().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().sponge()
                ));
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(ImmutableSet.copyOf(this.handle.getLoadedCollections().keySet()));
    }

    @Override
    public @NonNull SubjectReference newSubjectReference(@NonNull String collectionIdentifier, @NonNull String subjectIdentifier) {
        Objects.requireNonNull(collectionIdentifier, "collectionIdentifier");
        Objects.requireNonNull(subjectIdentifier, "subjectIdentifier");

        // test the identifiers
        String collection = collectionIdentifier.toLowerCase(Locale.ROOT);
        if (collection.equals("user") && !this.handle.getUserSubjects().getIdentifierValidityPredicate().test(subjectIdentifier)) {
            throw new IllegalArgumentException("Subject identifier '" + subjectIdentifier + "' does not pass the validity predicate for the user subject collection");
        } else if (collection.equals("group") && !this.handle.getGroupSubjects().getIdentifierValidityPredicate().test(subjectIdentifier)) {
            throw new IllegalArgumentException("Subject identifier '" + subjectIdentifier + "' does not pass the validity predicate for the group subject collection");
        }

        // obtain a reference
        return this.handle.getReferenceFactory().obtain(collectionIdentifier, subjectIdentifier);
    }

    @Override
    public PermissionDescription.Builder newDescriptionBuilder(@NonNull Object o) {
        Optional<PluginContainer> container = Sponge.getGame().getPluginManager().fromInstance(o);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Couldn't find a plugin container for " + o.getClass().getSimpleName());
        }

        return new DescriptionBuilder(this.handle, container.get());
    }

    @Override
    public @NonNull Optional<PermissionDescription> getDescription(@NonNull String s) {
        return this.handle.getDescription(s).map(LPPermissionDescription::sponge);
    }

    @Override
    public @NonNull Collection<PermissionDescription> getDescriptions() {
        return this.handle.getDescriptions().stream().map(LPPermissionDescription::sponge).collect(ImmutableCollectors.toSet());
    }

    @Override
    public void registerContextCalculator(@NonNull ContextCalculator<Subject> contextCalculator) {
        this.handle.registerContextCalculator(contextCalculator);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof PermissionServiceProxy && this.handle.equals(((PermissionServiceProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api7.PermissionServiceProxy(handle=" + this.handle + ")";
    }
}
