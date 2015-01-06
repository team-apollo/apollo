var gulp = require("gulp");
var touch = require("touch");

gulp.task("touch-client", function(){
    //this is probably not the best way to do it but whatever. node is
    //a hack anyway
    touch("src/cljs/cljspazzer/client/pages.cljs", {});
});

gulp.task("watch-templates", function(){
    gulp.watch("resources/templates/*.html", ["touch-client"]);
});

gulp.task("default", ["watch-templates"]);
