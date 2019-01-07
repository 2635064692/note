package com.mapgis.apidb.controller;

import com.mapgis.apidb.service.MongoDBHelper;
import com.mapgis.apidb.service.MultiPartFileSender;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/file")
public class FileDBController {
    @Autowired
    private MongoDBHelper mongoDBHelper;

    private final Logger logger = LoggerFactory.getLogger(FileDBController.class);

    /***
     * 文件上传
     * @param file
     * @throws Exception
     */
    @ApiOperation(
            value = "Upload a file into the system",
            notes = "Single endpoint to upload files.",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "file",
                    value = "File to upload",
                    required = true,
                    dataType = "file",
                    paramType = "form")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "File upload success"),
            @ApiResponse(code = 403, message = "Operation forbidden"),
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    public String upload(@RequestParam("file") MultipartFile[] file) throws Exception {

        for(MultipartFile f :file){

            if(!f.getOriginalFilename().equals("")){
                mongoDBHelper.uploadFile(f.getInputStream(), f.getOriginalFilename());
                return f.getOriginalFilename();
            }else
                return "null";
        }
        return null;
    }

    /***
     * 文件下载
     * @param name
     * @param response
     * @param request
     * @throws Exception
     */
    @RequestMapping(value = "/download/{filename}",method = RequestMethod.GET)
    public void downloadByMe(@PathVariable("filename") String name, HttpServletResponse response, HttpServletRequest request) throws Exception {
        GridFsResource file = mongoDBHelper.getFileResource(name);
        MultiPartFileSender.fromGridFsResource(file).with(request).with(response).setDisposition("attachment").serveContent();
        System.out.println(file.getFilename()+"----"+file.contentLength()+"----");
    }

    /***
     * 获取文件内容
     * @param filename
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/content/{filename}",method = RequestMethod.GET)
    public void serveFile(@PathVariable("filename") String filename,HttpServletResponse response,HttpServletRequest request) throws Exception {
        GridFsResource file = mongoDBHelper.getFileResource(filename);
        MultiPartFileSender.fromGridFsResource(file).with(request).with(response).setDisposition("inline").serveContent();
    }

    /*private ResponseEntity serveFile(GridFsResource file) throws IOException {
        if (file == null) {
            logger.error("kong de ");
            return null;
        }
        String filename = file.getFilename();
        String type = filename.substring(filename.lastIndexOf(".") + 1, filename.length()).toLowerCase();

        String contentType = null;
        if(type.equals("png")||type.equals("jpg")||type.equals("bmp")) {
             contentType = "image/"+type;
        }else
            contentType = "text/plain";


        return ResponseEntity
                .ok()
                .contentLength(file.contentLength())
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(file.getInputStream()));
    }*/
}