<#import "../../freemarker/main-template.ftl" as u>

<@u.page>
<div class="page-header top5">
    <div class="row text-center">
        <div class="header-group">
            <h1>@UtilityClass</h1>

            <h3>Utility, metility, wetility! Utility classes for the masses.</h3>

            <p>
                <code>@UtilityClass</code> was introduced as an experimental feature in lombok v1.16.2.
            </p>
        </div>
    </div>
    <div class="row">
        <h3>Experimental</h3>

        <p>
            Experimental because:
        <ul>
            <li>Some debate as to whether its common enough to count as boilerplate.</li>
        </ul>
        Current status: <em>positive</em> - Currently we feel this feature may move out of experimental status with no
        or minor changes soon.
    </div>
    <div class="row">
        <h3>Overview</h3>

        <p>
            A utility class is a class that is just a namespace for functions. No instances of it can exist, and all its
            members
            are static. For example, <code>java.lang.Math</code> and <code>java.util.Collections</code> are well known
            utility classes. This annotation automatically turns the annotated class into one.
        </p>

        <p>
            A utility class cannot be instantiated. By marking your class with <code>@UtilityClass</code>, lombok will
            automatically
            generate a private constructor that throws an exception, flags as error any explicit constructors you add,
            and marks
            the class <code>final</code>. If the class is an inner class, the class is also marked <code>static</code>.
        </p>

        <p>
            <em>All</em> members of a utility class are automatically marked as <code>static</code>. Even fields and
            inner classes.
        </p>
    </div>
    <@u.comparison />
    <div class="row">
        <h3>Small print</h3>

        <div class="smallprint">
            <p>
                There isn't currently any way to create non-static members, or to define your own constructor. If you
                want to instantiate
                the utility class, even only as an internal implementation detail, <code>@UtilityClass</code> cannot be
                used.
            </p>
        </div>
    </div>
</div>
</@u.page>
