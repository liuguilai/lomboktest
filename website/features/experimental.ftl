<#import "../freemarker/main-template.ftl" as u>

<@u.page>
<div class="page-header top5">
    <div class="row text-center">
        <h1>Lombok experimental features.</h1>
    </div>
    <div class="row">
        Experimental features are available in your normal lombok installation, but are not as robustly supported as
        lombok's main features.
        In particular, experimental features:
        <ul>
            <li>Are not tested as well as the core features.</li>
            <li>Do not get bugs fixed as quickly as core features.</li>
            <li>May have APIs that will change, possibly drastically if we find a different, better way to solve the
                same problem.
            </li>
            <li>May disappear entirely if the feature is too difficult to support or does bust enough boilerplate.</li>
        </ul>
        Features that receive positive community feedback and which seem to produce clean, flexible code will eventually
        become accepted
        as a core feature and move out of the experimental package.
        <div class="row">
            <@u.feature title="@Accessors" code="accessors">
                A more fluent API for getters and setters.
            </@u.feature>

            <@u.feature title="@ExtensionMethod" code="extension-method">
                Annoying API? Fix it yourself: Add new methods to existing types!
            </@u.feature>

            <@u.feature title="@FieldDefaults" code="field-defaults">
                New default field modifiers for the 21st century.
            </@u.feature>

            <@u.feature title="@Delegate" code="delegate">
                Don't lose your composition.
            </@u.feature>

            <@u.feature title="@Wither" code="wither">
                Immutable 'setters' - methods that create a clone but with one changed field.
            </@u.feature>

            <@u.feature title="onMethod= / onConstructor= / onParam=" code="on-x">
                Sup dawg, we heard you like annotations, so we put annotations in your annotations so you can annotate
                while you're annotating.
            </@u.feature>

            <@u.feature title="@UtilityClass" code="utility">
                Utility, metility, wetility! Utility classes for the masses.
            </@u.feature>
        </div>
    </div>
    <div class="row">
        <h3 class="text-center">Supported configuration keys:</h3>

        <div class="row">
            <p>
                <code>lombok.experimental.flagUsage</code> = [<code>warning</code> | <code>error</code>] (default: not
                set)
            </p>

            <p>Lombok will flag any usage of any of the features listed here as a warning or error if configured.</p>
        </div>
    </div>
    <div class="row">
        <h3 class="text-center">Putting the "Ex" in "Experimental": promoted or deleted experimental features.</h3>

        <div class="row">
            <@u.feature title="@Value: promoted" code="value">
                <code>@Value</code> has proven its value and has been moved to the main package.
            </@u.feature>
            <@u.feature title="@Builder: promoted" code="builder">
                <code>@Builder</code> is a solid base to build APIs on, and has been moved to the main package.
            </@u.feature>
        </div>
    </div>
</div>
</@u.page>
