<#import "../../freemarker/main-template.ftl" as u>


<@u.page>
<div class="page-header top5">
    <div class="row text-center">
        <div class="header-group">
            <h1>@Delegate</h1>

            <h3>Don't lose your composition.</h3>
        </div>
    </div>
    <div class="row">
        <p>
            @Delegate was introduced as feature in lombok v0.10. It was moved to the experimental package in
            lombok v1.14; the old version from the main lombok package is now deprecated.
        </p>
    </div>
    <div class="row">
        <h3>Experimental</h3>

        <p>
            Experimental because:
        <ul>
            <li>Not used that much</li>
            <li>Difficult to support for edge cases, such as recursive delegation.</li>
            <li>API is rather unfriendly; it would be a lot nicer if you can simply implement some methods and let
                <code>@Delegate</code> generate delegates for whatever you didn't manually implement, but due to
                issues with generics erasure this also can't be made to work without caveats.
        </ul>
        Current status: <em>negative</em> - Currently we feel this feature will not move out of experimental status
        anytime soon, and support for this feature may be dropped if future versions of javac or ecj make it
        difficult to continue to maintain the feature.
    </div>
    <div class="row">
        <h3>Overview</h3>

        <p>
            Any field or no-argument method can be annotated with <code>@Delegate</code> to let lombok generate
            delegate methods that forward the call to this field (or the result of invoking this method).
        </p>

        <p>
            Lombok delegates all <code>public</code> methods of the field's type (or method's return type), as well
            as those of its supertypes except for all
            methods declared in <code>java.lang.Object</code>.
        </p>

        <p>
            You can pass any number of classes into the <code>@Delegate</code> annotation's <code>types</code>
            parameter.
            If you do that, then lombok will delegate all <code>public</code> methods in those types (and their
            supertypes, except <code>java.lang.Object</code>) instead of looking at the field/method's type.
        </p>

        <p>
            All public non-<code>Object</code> methods that are part of the calculated type(s) are copied, whether
            or not you also wrote implementations for those methods. That would thus result in duplicate method
            errors. You can avoid these
            by using the <code>@Delegate(excludes=SomeType.class)</code> parameter to exclude all public methods in
            the excluded type(s), and their supertypes.
        </p>

        <p>
            To have very precise control over what is delegated and what isn't, write private inner interfaces with
            method signatures, then specify these
            private inner interfaces as types in <code>@Delegate(types=PrivateInnerInterfaceWithIncludesList.class,
            excludes=SameForExcludes.class)</code>.
        </p>
    </div>
    <@u.comparison />
    <div class="row">
        <h3>Supported configuration keys:</h3>
        <dl>
            <dt><code>lombok.delegate.flagUsage</code> = [<code>warning</code> | <code>error</code>] (default: not
                set)
            </dt>
            <dd>Lombok will flag any usage of <code>@Delegate</code> as a warning or error if configured.</dd>
        </dl>
    </div>
    <div class="overview">
        <h3>Small print</h3>

        <div class="smallprint">
            <p>
                When passing classes to the annotation's <code>types</code> or <code>excludes</code> parameter, you
                cannot include generics.
                This is a limitation of java. Use private inner interfaces or classes that extend the intended type
                including the
                generics parameter to work around this problem.
            </p>

            <p>
                When passing classes to the annotation, these classes do not need to be supertypes of the field. See
                the example.
            </p>

            <p>
                <code>@Delegate</code> cannot be used on static fields or methods.
            </p>

            <p>
                <code>@Delegate</code> cannot be used when the calculated type(s) to delegate / exclude themselves
                contain <code>@Delegate</code> annotations; in other words, <code>@Delegate</code> will error if you
                attempt to use it recursively.
        </div>
    </div>
</div>
</@u.page>
