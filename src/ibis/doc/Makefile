TARGETS = progman.ps progman.pdf progman.html

all:		$(TARGETS)

distr:		$(TARGETS)

%.ps:		%.dvi
		dvips -o $@ $<

%.pdf:		%.dvi
		dvipdf $< $@

progman.dvi:	progman.tex

%.dvi:		%.tex
		latex $<
		latex $<

progman.html:	progman.tex
		latex2html -info "" -split 4 -show_section_numbers progman.tex

clean:
		rm -rf *.aux *.dvi *.log *.bbl *.blg */TMP

allclean:	clean
		rm -rf *.ps *.pdf progman
