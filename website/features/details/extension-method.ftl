<#import "../../freemarker/main-template.ftl" as u>

<@u.page>
<div class="page-header top5">
    <div class="row text-center">
        <div class="header-group">
            <h1>@ExtensionMethod</h1>

            <h3>Annoying API? Fix it yourself: Add new methods to existing types!</h3>

            <p>
                @ExtensionMethod was introduced as experimental feature in lombok v0.11.2.
            </p>
        </div>
    </div>
    <div class="row">
        <div class="experimental">
            <h3>Experimental</h3>

            <p>
                Experimental because:
            <ul>
                <li>High-impact on code style</li>
                <li>Really would like to ship with utility methods to expand common classes, but so far lombok doesn't
                    have a good distribution method for such runtime dependencies
                </li>
                <li>Affects quite a bit of eclipse, and auto-complete e.d. do not work yet in netbeans</li>
                <li>Should @ExtensionMethod be legal on methods? Should it be legal on packages?</li>
            </ul>
            Current status: <em>positive</em> - Currently we feel this feature may move out of experimental status with
            no or minor changes soon.
        </div>
        <div class="overview">
            <h3>Overview</h3>

            <p>
                You can make a class containing a bunch of <code>public</code>, <code>static</code> methods which all
                take at least 1
                parameter. These methods will extend the type of the first parameter, as if they were instance methods,
                using the
                <code>@ExtensionMethod</code> feature.
            </p>

            <p>
                For example, if you create <code>public static String toTitleCase(String in) { ... }</code>, you can use
                the
                <code>@ExtensionMethod</code> feature to make it look like the <code>java.lang.String</code> class has a
                method named
                <code>toTitleCase</code>, which has no arguments. The first argument of the static method fills the role
                of
                <code>this</code>
                in instance methods.
            </p>

            <p>
                All methods that are <code>public</code>, <code>static</code>, and have at least 1 argument whose type
                is not primitive, are
                considered extension methods, and each will be injected into the namespace of the type of the first
                parameter as if they were
                instance methods. As in the above example, a call that looks like: <code>foo.toTitleCase()</code> is
                replaced with
                <code>ClassContainingYourExtensionMethod.toTitleCase(foo);</code>. Note that it is actually not an
                instant
                <code>NullPointerException</code> if <code>foo</code> is null - it is passed like any other parameter.
            </p>

            <p>
                You can pass any number of classes to the <code>@ExtensionMethod</code> annotation; they will all be
                searched for
                extension methods. These extension methods apply for any code that is in the annotated class.
            </p>

            <p>
                Lombok does not (currently) have any runtime dependencies which means lombok does not (currently) ship
                with any useful
                extension methods so you'll have to make your own. However, here's one that might spark your
                imagination:
				<pre>public class ObjectExtensions {
	public static &lt;T&gt; or(T object, T ifNull) {
		return object != null ? object : ifNull;
	}
}</pre>
            With the above class, if you add <code>@ExtensionMethod(ObjectExtensions.class)</code> to your class
            definition, you can write:
			<pre>String x = null;
System.out.println(x.or("Hello, World!"));</pre>
            The above code will not fail with a <code>NullPointerException</code>; it will actually output <code>Hello,
            World!</code>
        </div>
        <@u.comparison />
        <div class="row">
            <h3>Supported configuration keys:</h3>
            <dl>
                <dt><code>lombok.extensionMethod.flagUsage</code> = [<code>warning</code> | <code>error</code>]
                    (default: not set)
                </dt>
                <dd>Lombok will flag any usage of <code>@ExtensionMethod</code> as a warning or error if configured.
                </dd>
            </dl>
        </div>
        <div class="overview">
            <h3>Small print</h3>

            <div class="smallprint">
                <p>
                    Calls are rewritten to a call to the extension method; the static method itself is not inlined.
                    Therefore, the
                    extension method must be present both at compile and at runtime.
                </p>

                <p>
                    Generics is fully applied to figure out extension methods. i.e. if the first parameter of your
                    extension method is
                    <code>List&lt;? extends String&gt;</code>, then any expression that is compatible with that will
                    have your extension method,
                    but other kinds of lists won't. So, a <code>List&lt;Object&gt;</code> won't get it, but a <code>List&lt;String&gt;</code>
                    will.
                </p>
            </div>
        </div>
    </div>
</div>
</@u.page>
