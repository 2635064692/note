package com.mapgis.apidb.service;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 10/02/15.
 * See full code here : https://github.com/davinkevin/Podcast-Server/blob/d927d9b8cb9ea1268af74316cd20b7192ca92da7/src/main/java/lan/dk/podcastserver/utils/multipart/MultipartFileSender.java
 *
 * Notes:
 *
 * - Spring used to support this functionality out of the box, they dropped it and might bring in back in Spring 5
 *          https://jira.spring.io/browse/SPR-10805
 *
 * - This class is NOT BASED on Kevin's latest version. It is based from an older version he posted 2yrs ago but
 *   people had success so I went with it. We could move to the latest if we see any benefits (perhaps performance).
 *   Here's the version I worked with:
 *          https://gist.github.com/davinkevin/b97e39d7ce89198774b4
 *
 * - Again, this is working and should be used with out modifications:
 *     Ultimately, it only cares about a few fields and that it
 *     has an InputStream to work with. (See "serveContent()" and line 270)
 *
 * - This relies on Apache Tika. Tika helps to determine what mimetype the file in question is. Originally, Kevin created a "Tika/MimeType"
 *    utility class. I'm just using the basic, "guess the mimetype by filename" API, for a quick and dirty.
 *
 *  - I like the builder pattern used here so I expanded on it.
 */
@SuppressWarnings("PMD")
public class MultiPartFileSender {

    private final Logger logger = LoggerFactory.getLogger(MultiPartFileSender.class);

    private static final int DEFAULT_BUFFER_SIZE = 20480; // ..bytes = 20KB.

    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.

    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";


    private Path filepath;

    private GridFsResource gridFsResource;

    private boolean useFile;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private String disposition;

    public static MultiPartFileSender fromGridFsResource(GridFsResource gridFsResource){
        return new MultiPartFileSender().setGridFsResource(gridFsResource);
    }

    //** internal setter **//
    private MultiPartFileSender setFilepath(Path filepath) {
        this.useFile = true;
        this.filepath = filepath;
        return this;
    }

    /**
     * New addition for GridFS compatability
     * @param gridFsResource
     * @return
     */
    private MultiPartFileSender setGridFsResource(GridFsResource gridFsResource){
        this.useFile = false;
        this.gridFsResource = gridFsResource;
        return  this;
    }

    public MultiPartFileSender with(HttpServletRequest httpRequest) {
        request = httpRequest;
        return this;
    }

    public MultiPartFileSender with(HttpServletResponse httpResponse) {
        response = httpResponse;
        return this;
    }

    public MultiPartFileSender setDisposition(String disposition){
        this.disposition = disposition;
        return this;
    }

    /**
     * Main entry point to get the content based on configuration
     * I basically pulled this out of the original serveResource(..) because I felt like that method
     * did too much.
     *
     * "An engine should not have to fetch its own gas, it should be injected with gas"
     *
     * @throws Exception
     */
    public void serveContent() throws Exception{
        if (response == null || request == null) {
            return;
        }
        Long length;
        String fileName ;
        Object lastModifiedObj;
        long lastModified;
        if(useFile) {
            /*ORIGINAL CODE*/
            if (!Files.exists(filepath)) {
                logger.error("File doesn't exist at URI : {}", filepath.toAbsolutePath().toString());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            length = Files.size(filepath);
            fileName = filepath.getFileName().toString();
            lastModifiedObj = Files.getLastModifiedTime(filepath);
            lastModified = LocalDateTime.ofInstant(((FileTime)lastModifiedObj).toInstant(), ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);
        } else {
            /*GRIDFS Addition*/
            length = gridFsResource.contentLength();
            fileName = gridFsResource.getFilename();
            fileName = new String(fileName.getBytes(), "ISO-8859-1");
            lastModified = gridFsResource.lastModified();
            //lastModified = LocalDateTime.ofInstant(((Date) lastModifiedObj).toInstant(), ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);
        }


        if (StringUtils.isEmpty(fileName)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String contentType = contentType(fileName);

        /*We are ready to serve..*/
        serveResource(length, fileName, lastModified, contentType);
    }

    /**
     * Actually does the heavy lifting given the parameters
     * @param length
     * @param fileName
     * @param lastModified
     * @param contentType
     * @throws Exception
     */
    private void serveResource( Long length, String fileName, long lastModified, String contentType) throws Exception {


        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        logger.debug("range is "+range);

        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set content disposition.

        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        logger.debug("Content-Type : {}", contentType);
        // Initialize response.
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        logger.debug("Content-Disposition : {}", disposition);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", fileName);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);

        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.

        /*
            Switcharoo Here Here!
         */
        InputStream source;
        if(useFile){
            source = new BufferedInputStream(Files.newInputStream(filepath));
        } else {
            source = gridFsResource.getInputStream();
        }

        //try ();
        try (InputStream input = new BufferedInputStream(source);
             OutputStream output = response.getOutputStream()) {

                // Return full file.
                logger.debug("Return full file");
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + full.start + "-" + full.end + "/" + full.total);
                response.setHeader("Content-Length", String.valueOf(full.length));
                Range.copy(input, output, length, full.start, full.length);
        }

    }

    public String contentType(String fileName){

        String type = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
        switch (type){
            case "txt":
                type = "text/plain";
                break;
            case "jpg":
                type = "image/jpeg";
                break;
            case "xlsx":
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            case "png":
                type = "image/png";
                break;
        }
        return type;
    }



    private static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

        private static void copy(InputStream input, OutputStream output, long inputSize, long start, long length) throws IOException {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int read;

            if (inputSize == length) {
                // Write full range.
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                    output.flush();
                }
            } else {
                input.skip(start);
                long toRead = length;

                while ((read = input.read(buffer)) > 0) {
                    if ((toRead -= read) > 0) {
                        output.write(buffer, 0, read);
                        output.flush();
                    } else {
                        output.write(buffer, 0, (int) toRead + read);
                        output.flush();
                        break;
                    }
                }
            }
        }
    }
}
