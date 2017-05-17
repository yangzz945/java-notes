package com.youku.recommend.debug;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonFilter;
import org.codehaus.jackson.map.ser.FilterProvider;
import org.codehaus.jackson.map.ser.impl.SimpleBeanPropertyFilter;
import org.codehaus.jackson.map.ser.impl.SimpleFilterProvider;

import com.youku.recommend.caculate.pojo.RecContext;
import com.youku.recommend.calculate.util.StringUtil;
import com.youku.recommend.command.CalculatorCommand;
import com.youku.recommend.dataservice.pojo.Recommendable;
import com.youku.recommend.pojo.YoukuVideoPackedRecommendable;
import com.youku.recommend.pojo.request.RecommendRequest;

/**
 * @author yangzhizhong
 * 在线debug 工具类
 * 
 * 1、Command中使用 
 * 	public static void put(Object thisObj,RecommendRequest  request,String key,Object value)
 * 
 * 2、 Calculator中使用
 * 
 * public static void put(Object thisObj,RecContext recContext,String key,Object value) {
 */
public class DebugUtil {
	
	// 忽略 Recommendable 中的 @JsonIgnore，否则weight无法输出
	private  static ThreadLocal<ObjectMapper> tlMapper = new ThreadLocal<ObjectMapper>() {
		@Override
		protected ObjectMapper initialValue() {
			return new ObjectMapper();
		}
	};
	
	private static ThreadLocal<PrefixManager> tlPrefixManager = new ThreadLocal<PrefixManager>() {
		@Override
		protected PrefixManager initialValue() {
			return new PrefixManager();
		}
	};
	
	private static final String FILTER_BY_FIELDNAME="FilterByFieldName";
	/**
	 *  BaseServlet 最后阶段执行，清除threadlocal中的变量，否则可能导致内存泄露
	 */
	public static void clear() {
		if (null != tlMapper) {
			tlMapper.remove();
		}
		
		if (null != tlPrefixManager) {
			tlPrefixManager.remove();
		}
	}
	
	/**
	 * @param thisObj  当前对象  this ,如果是静态类，写 class对象
	 * @param request  当前请求
	 * @param key      key  从 DebugConstants 中取
	 * @param value    value
	 */
	public static void put(Object thisObj,RecommendRequest  request,String key,Object value) {
		if (null!=request && null!=thisObj && null!=key && null!=value) {
		
			if (request.isDebug() && null!=request.getDebugInfo() ) {
				try {
					Set<String> fbk = Collections.EMPTY_SET;
					Set<String> fbv = Collections.EMPTY_SET;
					
					Set<String> fbf = Collections.EMPTY_SET;
					Integer top = Integer.valueOf(-1);
					if (null!=request.getDebugParaMap()&&request.getDebugParaMap().size()>0) {
						fbk = StringUtil.strTostrSet(request.getDebugParaMap().get(DebugConstants.FBK), StringUtil.UNDERLINE);
						fbv = StringUtil.strTostrSet(request.getDebugParaMap().get(DebugConstants.FBV), StringUtil.UNDERLINE);		
						
						fbf = StringUtil.strTostrSet(request.getDebugParaMap().get(DebugConstants.FBF), StringUtil.UNDERLINE);
						
						String topStr = request.getDebugParaMap().get(DebugConstants.TOP);
						if (StringUtils.isNotBlank(topStr)) {
							top = Integer.valueOf(topStr);
						}
					}	
					
					String prefix = tlPrefixManager.get().getPrefix(thisObj);
					String realKey = prefix+key;
					
					if (isLike(fbk,realKey)) {
						//String strValue = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getFilterValue(value));
						Object filterValue = getFilterValue(fbv,value,top);
						//System.out.println("fvalue:"+filterValue);
						if (null!=filterValue && StringUtils.isNotEmpty(filterValue.toString())) {
							String formatValue = StringUtils.EMPTY;
							//all fields need output 
							if (fbf.isEmpty()) {
								ObjectMapper om = tlMapper.get();
								om.configure(SerializationConfig.Feature.USE_ANNOTATIONS, false);
								formatValue = om.writerWithDefaultPrettyPrinter().writeValueAsString(filterValue);
							} else { //only output fbf fields 
								ObjectMapper om = tlMapper.get();
								om.getSerializationConfig().addMixInAnnotations(Object.class, PropertyFilterMixIn.class);  
								FilterProvider filters = new SimpleFilterProvider().addFilter(FILTER_BY_FIELDNAME,SimpleBeanPropertyFilter.filterOutAllExcept(fbf));
								formatValue=om.writer(filters).withDefaultPrettyPrinter().writeValueAsString(filterValue);
							}
							formatValue = StringUtils.removeStart(formatValue, StringUtil.DOUBLE_QUOTE);
							formatValue = StringUtils.removeEnd(formatValue, StringUtil.DOUBLE_QUOTE);
							request.getDebugInfo().put(realKey, formatValue);
						}
					}
	
				} catch(Exception ex) {
					ex.printStackTrace();
				}
				
			}
		}
	}

	/**
	 * @param thisObj  当前对象  this ,如果是静态类，写 class对象
	 * @param recContext 当前的 recContext
	 * @param key    key  当前对象  this ,如果是静态类，写 class对象
	 * @param value  value 
	 */
	public static void put(Object thisObj,RecContext recContext,String key,Object value) {
		if (null!=recContext && null!=thisObj && null!=key && null!=value) { 
			put(thisObj,(RecommendRequest)recContext.getRecommendRequest(), key, value);
		}
	}
	
	// 没有debug参数的时候，认为是全部输出的，所以wordSet size为0时，islike = true
	private static boolean isLike(Set<String> wordSet,String realKey) {
		boolean islike = false;
		if (wordSet.size()>0) {
			for (String searchKey : wordSet) {
					if (StringUtils.containsIgnoreCase(realKey, searchKey)) {
						//String strValue = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
						islike = true;
						break;
					}
				}
		} else {
			islike = true;
		}
		return islike;
	}
	
/*	public static Object getFilterValue(Set<String> wordSet,Object obj) {
		return getFilterValue(wordSet,obj,-1);
	}*/
	
	private static Object getFilterValue(Set<String> wordSet,Object obj,int top) {
/*		if (null == wordSet || wordSet.size()==0) {
			return obj;
		}*/
		
		//String value = StringUtils.EMPTY;
		Object returnObj =null;
		if (null!=obj ) {
			
			if (obj instanceof List) {
				List result = new ArrayList();
				
				for (Object o : (List) obj) {
					if (isLike(wordSet,o.toString())) {
						result.add(o);
					}
				}
				
				List subList = result;
				if (top>0) {
					subList = result.subList(0, Math.min(top, result.size()));
				}
				
				if (subList.size()>0) {
					returnObj = subList;
				}
			} else {
				if (isLike(wordSet,obj.toString())) {
					returnObj = obj;
				}
			}
		}
		return returnObj;
	}
	
	 
	@JsonFilter(FILTER_BY_FIELDNAME)
	static class PropertyFilterMixIn {}  
	
	static class PrefixManager {

		private  Hashtable<String,Map<Integer,String>> ht = new Hashtable();
		//private  RecommendRequest lastRequest = null ;
		
		public  String getPrefix(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
			//obj.
			Class cls = obj.getClass();
			String className;

			if (!"Class".equals(cls.getSimpleName())) {
				className = BeanUtils.getSimpleProperty(cls, "name");
			} else {
				className = BeanUtils.getSimpleProperty(obj, "name");
			}
			
			Integer objHashCode = obj.hashCode();
			StringBuilder prefixSb = new StringBuilder();
			String prefix = null;
			int objCount = 0;
			//synchronized(ht) {
				//String geneKey = ;
				Map<Integer,String> prefixMap = ht.get(className);
				if (null == prefixMap || prefixMap.size()==0) {
					prefixMap = new HashMap<Integer,String> ();
					ht.put(className, prefixMap);
				} else {
					objCount = prefixMap.size()+1;
				}
				
				
				prefix = prefixMap.get(objHashCode);
				if (null == prefix) {
					prefixSb.append(className).append("_");
					if (objCount>0) {
						prefixSb.append(objCount).append("_");
					}
					
					
					//added @20160824 
					if (obj instanceof CalculatorCommand) {
						try {
							Object calName = BeanUtils.getSimpleProperty(obj, "calName");//.get(obj, "calName");
							if(null!=calName) {
								prefixSb.append(calName).append("_");
							}
						} catch(Throwable t) {
							t.printStackTrace();
						}
					}
					
					prefix = prefixSb.toString();
					prefixMap.put(objHashCode, prefix);
				}
				
			return prefix;
		}

	}
	
	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
/*		RecommendRequest request = new FeedRequest();
		request.setDebug(true);
		request.setDebugParam("");
		DebuggerUtil util = new DebuggerUtil();
		DebuggerUtil.put(util, request, "end", "end");
		DebuggerUtil util1 = new DebuggerUtil();
		DebuggerUtil.put(DebuggerUtil.class, request, "end", "end1");
		
		DebuggerUtil.put(DebuggerUtil.class, request, "cost", "10");
		
		System.out.println(request.getDebugInfo());
		
		 request = new FeedRequest();
		request.setDebug(true);
		request.setDebugParam("");
		DebuggerUtil.put(DebuggerUtil.class, request, "end", "end1");
		
		DebuggerUtil.put(DebuggerUtil.class, request, "cost", "10");
		
		System.out.println(request.getDebugInfo());*/
		Recommendable rec = new Recommendable();
		rec.setId("22");
		YoukuVideoPackedRecommendable recom =new YoukuVideoPackedRecommendable(rec);
		//recom.setId("111");
		recom.setArea("11");
		List<YoukuVideoPackedRecommendable> list = new ArrayList();
		list.add(recom);
		ObjectMapper om = new ObjectMapper();
		//om.configure(SerializationConfig.Feature.USE_ANNOTATIONS, false);
		//om.configure(SerializationConfig.Feature.USE_ANNOTATIONS, false);
		//om.disable(@JsonIgnore);
		//JacksonAnnotationIntrospector introspector = new JacksonAnnotationIntrospector();
		//introspector.disable(@JsonIgnore);
		//om.setAnnotationIntrospector(introspector);
		//om.disable(MapperFeature.USE_ANNOTATIONS);
		
		//om.getSerializationConfig().addMixInAnnotations(Object.class, PropertyFilterMixIn.class);  
		
		//String midObj = om.writeValueAsString(list);
		//om.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
		//om.configure(SerializationConfig.Feature.USE_ANNOTATIONS, false);
		Set<String> set =new HashSet<String>();
		set.add("id");
		set.add("dma");
		set.add("weight");
		set.add("alginfo");
		//set.add("wrappedItem");
		
		om.getSerializationConfig().addMixInAnnotations(Object.class, PropertyFilterMixIn.class);
		SimpleBeanPropertyFilter fExcep =SimpleBeanPropertyFilter.filterOutAllExcept(set);//new SimpleBeanPropertyFilter.FilterExceptFilter(set);//.serializeAllExcept(ignorableFieldNames);
		FilterProvider filters = new SimpleFilterProvider()  
	      .addFilter(FILTER_BY_FIELDNAME,fExcep);  
		
		//Object obj = om.readValue(midObj,Map.class);
		String res=om.writer(filters).withDefaultPrettyPrinter().writeValueAsString(list);
		System.out.println(1);
		System.out.println(res);
		
		//om.getSerializationConfig().addMixInAnnotations(Object.class, PropertyFilterMixIn.class);
		//SimpleBeanPropertyFilter fExcep1 =SimpleBeanPropertyFilter.filterOutAllExcept(set);//new SimpleBeanPropertyFilter.FilterExceptFilter(set);//.serializeAllExcept(ignorableFieldNames);
		///FilterProvider filters1 = new SimpleFilterProvider()  
	    //  .addFilter(FILTER_BY_FIELDNAME,fExcep1); 
		//String res1=om.writer(filters1).withDefaultPrettyPrinter().writeValueAsString((Object)list);
		
		//om.writer
		//String res = om.writerWithDefaultPrettyPrinter().writeValueAsString(recom);
		
		//System.out.println(res1);
		/*ObjectMapper om1 = new ObjectMapper();
		String res2 = om1.writerWithDefaultPrettyPrinter().writeValueAsString(recom);
		System.out.println(res2);
		
		RecommendRequest request = new FeedRequest();
		request.setPl(1);
		request.setDebug(true);
		request.setDebugParam("");
		RecContext recContext = request.buildRecContext();
		//DebugUtil.put(DebugUtil.class, recContext, "end", "end1");
		
		NewFilterDuplicateRemovalCalculatorCommand cmd = new NewFilterDuplicateRemovalCalculatorCommand();
		//String className, String initParam, String calCmdName
		CalculatorConfig calConf = new CalculatorConfig(1,"DummyCalculatorr","","NewFilterDuplicateRemovalCalculatorCommand");
		Object cls = Class.forName("com.youku.recommend.calculate.calculator.DummyCalculator").newInstance();
		calConf.setCal((ICalculator)cls);
		cmd.setCalculatorConfig(calConf);
		DebugUtil.put(cmd, recContext, "end", "end1");
		
		//DebuggerUtil.put(DebuggerUtil.class, request, "cost", "10");
		
		System.out.println(request.getDebugInfo()); */
	}
}
