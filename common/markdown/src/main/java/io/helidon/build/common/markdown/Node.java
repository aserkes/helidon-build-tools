/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common.markdown;

/**
 * The base class of all AST nodes ({@link Block} and inlines).
 */
abstract class Node {

    private Node parent = null;
    private Node firstChild = null;
    private Node lastChild = null;
    private Node prev = null;
    private Node next = null;

    public abstract void accept(Visitor visitor);

    public Node next() {
        return next;
    }

    public Node previous() {
        return prev;
    }

    public Node firstChild() {
        return firstChild;
    }

    public Node lastChild() {
        return lastChild;
    }

    public Node parent() {
        return parent;
    }

    protected void parent(Node parent) {
        this.parent = parent;
    }

    public void appendChild(Node child) {
        child.unlink();
        child.parent(this);
        if (this.lastChild != null) {
            this.lastChild.next = child;
            child.prev = this.lastChild;
            this.lastChild = child;
        } else {
            this.firstChild = child;
            this.lastChild = child;
        }
    }

    public void unlink() {
        if (this.prev != null) {
            this.prev.next = this.next;
        } else if (this.parent != null) {
            this.parent.firstChild = this.next;
        }
        if (this.next != null) {
            this.next.prev = this.prev;
        } else if (this.parent != null) {
            this.parent.lastChild = this.prev;
        }
        this.parent = null;
        this.next = null;
        this.prev = null;
    }

    public void insertAfter(Node sibling) {
        sibling.unlink();
        sibling.next = this.next;
        if (sibling.next != null) {
            sibling.next.prev = sibling;
        }
        sibling.prev = this;
        this.next = sibling;
        sibling.parent = this.parent;
        if (sibling.next == null) {
            sibling.parent.lastChild = sibling;
        }
    }

    public void insertBefore(Node sibling) {
        sibling.unlink();
        sibling.prev = this.prev;
        if (sibling.prev != null) {
            sibling.prev.next = sibling;
        }
        sibling.next = this;
        this.prev = sibling;
        sibling.parent = this.parent;
        if (sibling.prev == null) {
            sibling.parent.firstChild = sibling;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + toStringAttributes() + "}";
    }

    protected String toStringAttributes() {
        return "";
    }
}
