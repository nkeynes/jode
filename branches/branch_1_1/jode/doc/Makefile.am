## Input file for automake to generate the Makefile.in used by configure

PHP_FILES = \
applet.php \
bluesky.php \
download.php \
faq.php \
feedback.php \
history.php \
index.php \
license.php \
links.php \
usage.php

HTML_FILES = $(PHP_FILES:%.php=$(srcdir)/%.html)
# noinst_DATA = $(HTML_FILES)

EXTRA_DIST = $(PHP_FILES) $(notdir $(HTML_FILES)) \
header.inc \
menu.inc \
footer.inc \
a-logo.gif \
myproject.jos \
dasm_to_java.perl \
gimp/jode-logo.xcf \
jode-logo.gif

.PHONY: public_html_symlink

# The following rules require that you have an apache with php on
# localhost with standard user public_html directories and
# FollowSymLink enabled.

PUBLIC_HTML=$(HOME)/public_html
JODE_PHP_DIR=jode_php

public_html_symlink:
	rm -f $(PUBLIC_HTML)/$(JODE_PHP_DIR)
	@RELDIR=`pwd | sed s!^$(HOME)!..!`; \
	ln -sf $$RELDIR/$(srcdir) $(PUBLIC_HTML)/$(JODE_PHP_DIR); \
	echo Created symlink to $$RELDIR/$(srcdir).

footer.inc: public_html_symlink

$(srcdir)/%.html: %.php footer.inc header.inc menu.inc
	lynx -source http://localhost/~$(LOGNAME)/$(JODE_PHP_DIR)/$(notdir $<)?extension=html > $@
