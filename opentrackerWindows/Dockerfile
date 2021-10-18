FROM alpine

WORKDIR /tmp

# OPENTRACKER image for a usage on windows

FROM alpine

WORKDIR /tmp

# OPENTRACKER
RUN apk update &&\
    apk add --no-cache \
	gcc \
	g++ \
	make \
	git \
	cvs \
	zlib-dev \
	&& cvs -d :pserver:cvs@cvs.fefe.de:/cvs -z9 co libowfat \
	&& cd libowfat \
	&& make \
	&& cd ../ \
	&& git clone git://erdgeist.org/opentracker \
		&& cd opentracker \
		&& make

#delete unessecary packages
 RUN  apk del gcc g++ make git cvs zlib-dev
#delete libraries and keep only the compiled file
RUN  mkdir /open &&\
     mv /tmp/opentracker/opentracker /open &&\
     rm -rf /tmp/* \
     rm -rf /var/cache/apk/*

EXPOSE 6969

WORKDIR /open

CMD [ "./opentracker" ]