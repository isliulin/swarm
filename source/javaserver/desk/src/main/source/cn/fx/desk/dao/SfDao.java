package cn.fx.desk.dao;

import java.util.List;
import java.util.Map;


/**
 * @author sunxy
 * @version 2015-6-15
 * @des 
 **/
public interface SfDao {
	public void test();
	
	/**新增对象到指定的数据*/
	public void insert(List list,String objectName);
	/**获取Salesforce-->sobject*/
	public String getSalesforceObject(String instanceURL ,String accessToken,String apTableName)throws Exception;
	public String getSalesforceObjectByTime(String instanceURL ,String accessToken,String apTableName,String start,String end)throws Exception;
	public int getSfObjcetCountByTime(String instanceURL ,String accessToken,String apTableName,String start,String end)throws Exception;
	public String getSalesforceObjectByTime(String instanceURL ,String accessToken,String apTableName,String start,String end,int page,int perPage)throws Exception;
	
	public void updateSalesForceObject(String apUserId,String data,String collection) throws Exception;
	
	public String getSfUserAccount(String sfUid,String instanceURL ,String accessToken);
	
	public String getOpportunityFromSf(String instanceURL ,String accessToken)  throws Exception;
	public void addOpportunity(List<Map> list);
	
	public void dropCollection(String name);
	public void findOpportunity();
}
