package org.xjtu.framework.modules.user.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.xjtu.framework.core.base.model.Calculate;
import org.xjtu.framework.core.base.model.Project;
import org.xjtu.framework.core.base.model.User;
import org.xjtu.framework.core.util.UUIDGenerator;
import org.xjtu.framework.modules.user.dao.CalculateDao;
import org.xjtu.framework.modules.user.service.CalculateService;

@Service("calculateService")
public class CalculateServiceImpl implements CalculateService {

	private @Resource CalculateDao calculateDao;
	
	
	
	@Override
	public List<Calculate> listCalculatesByQuery(String searchText,String searchType, int pageNum, int pageSize) {
		List<Calculate> calculates = new ArrayList<Calculate>();
		if(StringUtils.isBlank(searchText)){
			calculates = calculateDao.pagnate(pageNum, pageSize);
		}else if(StringUtils.isBlank(searchType)){
			calculates = calculateDao.pagnate(pageNum, pageSize);
		}else{
			if(searchType.equals("name")){
				calculates = calculateDao.pagnateByCalculateName(searchText, pageNum, pageSize);
			}else if(searchType.equals("accurateUserId")){
				calculates = calculateDao.pagnateByAccurateUserId(searchText, pageNum, pageSize);
			}else{
				calculates = calculateDao.pagnate(pageNum, pageSize);
			}
		}
		return calculates;
	}

	@Override
	public int findTotalCountByQuery(String searchText, String searchType) {
		int num = 0;
		if(StringUtils.isBlank(searchText)){
			num = calculateDao.queryCount();
		}else if(StringUtils.isBlank(searchType)){
			num = calculateDao.queryCount();
		}else{
			if(searchType.equals("name")){
				num = calculateDao.queryCountByCalculateName(searchText);
			}else if(searchType.equals("accurateUserId")){
				num = calculateDao.queryCountByAccurateUserId(searchText);
			}else{
				num = calculateDao.queryCount();
			}
		}
		return num;
	}

	@Override
	public Calculate findCalculateById(String calculateId) {
		return calculateDao.queryCalculateById(calculateId);
	}

	@Override
	public void deleteCalculate(Calculate calculate) {
		calculateDao.removeCalculate(calculate);
	}

	@Override
	public void addCalculate(Calculate c) {
		calculateDao.persist(c);
	}

	@Override
	public Calculate findCalculateByCalculateNameAndUserId(String param,String id) {
		return calculateDao.queryCalculateByCalculateNameAndUserId(param,id);
	}

	@Override
	public List<Calculate> findCalculates() {
		return calculateDao.queryCalculates();
	}

	@Override
	public void doCopyCalculates(List<String> xmlIds) {
		for(int i=0;i<xmlIds.size();i++){
			Calculate calculate=calculateDao.queryCalculateById(xmlIds.get(i));
			if(calculate!=null){
				Calculate c=new Calculate();
				int num;
				num=calculateDao.queryCountcopyCalculateName(calculate.getXmlName());
				c.setId(UUIDGenerator.getUUID());
				c.setXmlName(calculate.getXmlName()+"-副本"+num);
				c.setXmlCreateTime(new Date());
				c.setXmlStatus(0);
				c.setXmlProgress(0);
				c.setXmlPriority(0);
				c.setXmlFilePath(calculate.getXmlFilePath());
				User user=calculate.getUser();
				c.setUser(user);
				Project project=calculate.getProject();
				c.setProject(project);
				calculateDao.persist(c);
			}
		}
	}


	
	@Override
	public Calculate findHeadQueuingCalculate() {
		return calculateDao.queryHeadQueuingJob();
		
	}

}
