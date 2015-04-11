<#import "../../freemarker/main-template.ftl" as u>

<@u.page>
<div class="page-header top5">
    <div class="row text-center">
        <div class="header-group">
            <h1>val</h1>

            <h3>Finally! Hassle-free final local variables.</h3>
        </div>
    </div>
    <div class="row">
        <h3>Overview</h3>

        <p>
            <em>NEW in Lombok 0.10: </em>You can use <code>val</code> as the type of a local variable declaration
            instead of actually writing the type. When you do this,
            the type will be inferred from the initializer expression. The local variable will also be made final.
            This feature works
            on local variables and on foreach loops only, not on fields. The initializer expression is required.
        </p>

        <p>
            <code>val</code> is actually a 'type' of sorts, and exists as a real class in the <code>lombok</code>
            package. You must import it for val to work (or use <code>lombok.val</code> as the type).
            The existence of this type on a local variable declaration triggers both the adding of the
            <code>final</code> keyword as well as copying the type of the initializing expression which overwrites
            the 'fake' <code>val</code> type.
        </p>

        <p>
            <em>WARNING: This feature does not currently work in NetBeans. We're working on fixing that.</em>
        </p>
    </div>
    <@u.comparison />
    <div class="row">
        <h3>Supported configuration keys:</h3>
        <dl>
            <dt><code>lombok.val.flagUsage</code> = [<code>warning</code> | <code>error</code>] (default: not set)
            </dt>
            <dd>Lombok will flag any usage of <code>val</code> as a warning or error if configured.</dd>
        </dl>
    </div>
    <div class="row">
        <h3>Small print</h3>

        <div class="smallprint">
            <p>
                For compound types, the most common superclass is inferred, not any shared interfaces. For example,
                <code>bool ? new HashSet() : new ArrayList()</code>
                is an expression with a compound type: The result is both <code>AbstractCollection</code> as well as
                <code>Serializable</code>. The type inferred will be
                <code>AbstractCollection</code>, as that is a class, whereas <code>Serializable</code> is an
                interface.
            </p>

            <p>
                In ambiguous cases, such as when the initializer expression is <code>null</code>,
                <code>java.lang.Object</code>
                is inferred.
            </p>
        </div>
    </div>
</div>
</@u.page>
