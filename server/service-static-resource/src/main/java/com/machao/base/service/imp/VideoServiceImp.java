package com.machao.base.service.imp;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.machao.base.handler.video.imp.FFmpegVideoHandler;
import com.machao.base.model.mq.QueueName;
import com.machao.base.model.mq.video.request.VideoConvertRequest;
import com.machao.base.model.mq.video.request.VideoDeleteRequest;
import com.machao.base.model.mq.video.request.VideoPlayListRequest;
import com.machao.base.model.mq.video.response.VideoDeleteResponse;
import com.machao.base.model.mq.video.response.VideoPlayListResponse;
import com.machao.base.model.persit.StaticResource;
import com.machao.base.service.StaticResourceService;
import com.machao.base.service.VideoService;
import com.machao.base.utils.StaticResourcePathUtils;

@Service
public class VideoServiceImp implements VideoService{
	private static final Logger logger = LoggerFactory.getLogger(VideoServiceImp.class);

	@Autowired
	private StaticResourceService staticResourceService;
	
	@Autowired
	private StaticResourcePathUtils staticResourcePathUtils;
	
	@Autowired
	private FFmpegVideoHandler ffmpegVideoHandler;

	@RabbitListener(queues = QueueName.VideoConvert)
	@Override
	public void convert(VideoConvertRequest videoConvertRequest) {
		StaticResource staticResource = videoConvertRequest.getStaticResource();
		try {
			File file = new File(staticResource.getPath());
			this.ffmpegVideoHandler.handle(file);
			
			staticResource.setHandled(true);
			this.staticResourceService.update(staticResource);
		} catch (Exception e) {
			logger.error("error to convert file {} to m3u8, exception: {}", staticResource.getPath(), e.getMessage());
			
			staticResource.setHandled(false);
			this.staticResourceService.update(staticResource);
		} 
	}

	@RabbitListener(queues = QueueName.VideoDelete)
	@Override
	public VideoDeleteResponse handle(VideoDeleteRequest videoDeleteRequest) {
		StaticResource staticResource = videoDeleteRequest.getStaticResource();
		
		try {
			File file = new File(staticResource.getPath());
			FileUtils.deleteDirectory(file.getParentFile());
			return new VideoDeleteResponse(true);
		} catch (IOException e) {
			logger.error("error to delete file {} to m3u8, exception: {}", staticResource.getPath(), e.getMessage());
			return new VideoDeleteResponse(false);
		}
	}
	
	@RabbitListener(queues = QueueName.VideoPlayList)
	@Override
	public VideoPlayListResponse handle(VideoPlayListRequest videoPlayListRequest) {
		StaticResource staticResource = videoPlayListRequest.getStaticResource();
		return new VideoPlayListResponse(staticResourcePathUtils.videoUrl(staticResource));
	}
	
}