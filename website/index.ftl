<#import "freemarker/main-template.ftl" as u>

<@u.page>
<div class="page-header top5">
    <div class="row text-center">
        <h1>Lombok Demo</h1>
    </div>
    <div class="video text-center">
        <video width="800" height="480" poster="images/poster.png" controls="controls" preload="none">
            <source src="videos/lombok.ogv" type="video/ogg"/>
            <source src="videos/lombok.mp4" type="video/mp4"/>
            <source src="videos/lombok-iPhone.m4v" type="video/mp4"/>
            <object id="player" classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000" width="800" height="500">
                <param name="movie" value="videos/player.swf?image=poster.png&amp;file=lombok.mp4"/>
                <param name="allowfullscreen" value="true"/>
                <!--[if !IE]>-->
                <object width="800" height="500" type="application/x-shockwave-flash"
                        data="videos/player.swf?image=poster.png&amp;file=lombok.mp4" allowfullscreen="true">
                    <!--<![endif]-->
                    <p>
                        <strong>No video playback capabilities detected.</strong>
                        Why not download it instead?<br/>
                        <a href="videos/lombok.mp4">MPEG4 / H.264 (Windows / Mac)</a> |
                        <a href="videos/lombok.ogv">Ogg Theora &amp; Vorbis ".ogv" (Linux)</a>
                    </p>

                    <p>
                        To play the video in the webpage, please do one of the following:
                    </p>
                    <ul>
                        <li>Upgrade to <a href="http://www.mozilla.org/firefox/">Firefox</a> or
                            <a href="http://www.google.com/chrome/">Chrome</a>
                        </li>
                        <li>Install <a href="http://get.adobe.com/flashplayer/">Adobe Flash</a></li>
                    </ul>
                    <!--[if !IE]>-->
                </object>
                <!--<![endif]-->
            </object>

        </video>
        <div class="row">
            <div class="text-center"><a href="http://jnb.ociweb.com/jnb/jnbJan2010.html">I can't see video. Show me a text
                and images based
                explanation and tutorial instead!</a></div>

        </div>
    </div>

    <script>swfobject.registerObject("player", "9.0.98", "videos/expressInstall.swf");</script>

</div>
</@u.page>
