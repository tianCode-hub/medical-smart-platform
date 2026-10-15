package com.medical.file.minio.service;

import java.io.InputStream;
import java.util.List;


public interface FileStorageService {


    /**
     *  上传图片文件
     * @param prefix  文件前缀
     * @param filename  文件名
     * @param inputStream 文件流
     * @return  文件全路径
     */
    public String uploadImgFile(String prefix, String filename,InputStream inputStream);

    /**
     *  上传html文件
     * @param prefix  文件前缀
     * @param filename   文件名
     * @param inputStream  文件流
     * @return  文件全路径
     */
     String uploadHtmlFile(String prefix, String filename,InputStream inputStream);

    /**
     * 删除文件
     * @param pathUrl  文件全路径
     */
     void delete(String pathUrl);

    /**
     * 下载文件
     * @param pathUrl  文件全路径
     * @return
     *
     */
     byte[]  downLoadFile(String pathUrl);

    /**
     * 批量下载：利用文件线程池并行下载多个文件，返回顺序与入参一致。
     * @param pathUrls  文件全路径列表
     * @return  文件字节流列表
     */
     List<byte[]> batchDownLoadFile(List<String> pathUrls);

}
