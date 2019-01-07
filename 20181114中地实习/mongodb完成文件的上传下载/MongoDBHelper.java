package com.mapgis.apidb.service;

import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.BsonValue;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Repository
public class MongoDBHelper {
    @Autowired
    Environment env;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    private boolean isNullOrEmpty(String val) {
        return val == null || val.length() == 0;
    }

    private void info(String context) {
        System.out.println(context);
    }

    public ObjectId uploadFile(MultipartFile multipartFile) throws Exception {
        return uploadFile(multipartFile, "");
    }

    public ObjectId uploadFile(MultipartFile multipartFile, String remoteFileName) throws Exception {
        // 获得提交的文件名
        String fileName = multipartFile.getOriginalFilename();

        if (!isNullOrEmpty(remoteFileName))
            fileName = remoteFileName;

        // 获得文件输入流
        InputStream ins = multipartFile.getInputStream();

        // 获得文件类型
        //String contentType = multipartFile.getContentType();

        return uploadFile(ins, fileName);
    }

    public ObjectId uploadFile(InputStream ins, String remoteFileName) {
        return uploadFile(ins, remoteFileName, true);
    }

    public ObjectId uploadFile(InputStream ins, String remoteFileName, boolean overwrite) {
        if (overwrite)
            deleteFile(remoteFileName);

        // 将文件存储到mongodb中,mongodb 将会返回这个文件的具体信息
        ObjectId objectId = gridFsTemplate.store(ins, remoteFileName);
        return objectId;
    }

    public GridFsResource getFileResource(String fileName) {
        return gridFsTemplate.getResource(fileName);
    }

    public void deleteFile(String fileName) {
        Query query = Query.query(Criteria.where("filename").is(fileName));

        gridFsTemplate.delete(query);
    }

    public void deleteFileById(BsonValue id) {
        Query query = Query.query(Criteria.where("_id").is(id.asString().getValue()));

        gridFsTemplate.delete(query);
    }

    public void deleteAll() {
        for (GridFSFile file : gridFsTemplate.find(new Query())) {
            deleteFileById(file.getId());
        }
    }

    public GridFSFindIterable findAll() {
        return gridFsTemplate.find(new Query());
    }

    public GridFSFile findOne(String remoteFileName) {
        return this.gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(remoteFileName)));
    }

}