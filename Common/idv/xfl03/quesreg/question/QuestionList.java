package idv.xfl03.quesreg.question;

import idv.xfl03.quesreg.config.MainConfig;
import idv.xfl03.quesreg.data.MainData;
import idv.xfl03.quesreg.data.ResourceFileTool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class QuestionList {
	public ArrayList<ArrayList<Question>> alq;//[type][index]
	public int[] sum;
	
	private final static QuestionFileFilter qff0=new QuestionFileFilter();;
	private final static QuestionFolderFilter qff1=new QuestionFolderFilter();
	private final static int SPLIT_BASIC=10000;
	private final static String SPLIT_LINE="{SPLITLINE}";
	
	private MainConfig mc;
	private MainData md;
	public QuestionList(File questionFolder,MainConfig mc,MainData md){
		this.mc=mc;
		this.md=md;
		
		//init arrays
		sum=new int[mc.questionTypes];
		alq=new ArrayList<ArrayList<Question>>();
		alq.add(null);
		for(int i=1;i<=mc.questionTypes;i++){
			alq.add(new ArrayList<Question>());
			sum[i]=0;
		}

		//init lists
		File[] subFolder=questionFolder.listFiles(qff1);
		if(subFolder.length==0){
			//First time
			System.out.println("Init Questions!");
			ResourceFileTool rft=this.md.rft;
			for(int i=1;i<=10;i++){
				//en
				File t=md.getSubFile(questionFolder, "/official-default-en/"+i+".txt");
				t.getParentFile().mkdirs();
				try {
					t.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				rft.resourceFileCopy("/resource/question/official-default-en/"+i+".txt",t);
				
				//zh-cn
				t=md.getSubFile(questionFolder, "/official-default-zh-cn/"+i+".txt");
				t.getParentFile().mkdirs();
				try {
					t.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				rft.resourceFileCopy("/resource/question/official-default-zh-cn/"+i+".txt", t);
			}
			for(int i=1;i<=5;i++){
				//Anti Small Children
				File t=md.getSubFile(questionFolder, "/anti-little-child-zh-cn/"+i+".txt");
				t.getParentFile().mkdirs();
				try {
					t.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				rft.resourceFileCopy("/resource/question/anti-little-child-zh-cn/"+i+".txt", t);
			}
			subFolder=questionFolder.listFiles(qff1);
		}
		
		for(File f : subFolder){
			if(!f.getAbsolutePath().endsWith(mc.language)){
				continue;
			}
			File[] subFile=f.listFiles(qff0);
			for(File f1 : subFile){
				Question q=new Question(f1,mc);
				alq.get(q.type).add(q);
				//System.out.println(q.type+" "+q.question);
				sum[q.type]++;
			}
		}
		
		for(int i=1;i<=mc.questionTypes;i++){
			if(sum[i]<mc.questionNumber[i]){
				System.out.println("TYPE "+i+" "+sum[i]+"/"+mc.questionNumber[i]);
				for(int j=sum[i];j<mc.questionNumber[i];j++){
					alq.get(i).add(new Question(i));
					//System.out.println(i+" blank");
				}
			}
		}
		
	}
	
	//For Database and display (like APIHandler)
	public String getRandomQuestions(){
		//System.out.println("getRandomQuestions");
		StringBuilder sb=new StringBuilder();
		for(int i=1;i<=mc.questionTypes;i++){
			System.out.println(i);
			int questionNumber=mc.questionNumber[i];
			if(questionNumber==0){
				//System.out.println(i+" Needs Nothing");
				continue;
			}
			int[] id=new int[questionNumber];//Question List
			System.out.println(mc.questionNumber.length+" "+id.length+" "+i);
			
			for(int j=0;j<questionNumber;j++){
				//int max=alq.get(i).size();
				int temp=i*SPLIT_BASIC+getRandom(1,questionNumber);//Get Random Question
				int loopCount=0;
				while(isUsed(id,temp)&&loopCount<=questionNumber){//If Question has already in the list
					loopCount++;
					temp=i*10000+getRandom(1,alq.get(i).size()-1);//get random again
				}
				if(isUsed(id,temp)){
					//If Too much Loop Times
					for(int k=i*SPLIT_BASIC+1;i<=i*SPLIT_BASIC+questionNumber;i++){//Find a question not in the list
						if(!isUsed(id,k)){
							temp=k;
							break;
						}
					}
				}
				//System.out.println(temp+" "+max);
				id[j]=temp;
			}
			if(sb.length()>0){
				sb.append(",");
			}
			sb.append(intsToString(id));
		}
		return sb.toString();
	}
	
	public String getQuestion(String id){
		Question q=getQuestion(Integer.parseInt(id));
		return q.question+SPLIT_LINE+q.a+SPLIT_LINE+q.b+SPLIT_LINE+q.c+SPLIT_LINE+q.d;
	}
	public Question getQuestion(int id){
		int type=id/SPLIT_BASIC;
		int i=id%SPLIT_BASIC;
		return alq.get(type).get(i-1);
	}
	
	public int[] getIds(String ids){
		String[] temp=ids.split(",");
		int[] id=new int[temp.length];
		for(int i=0;i<temp.length;i++){
			id[i]=Integer.parseInt(temp[i]);
		}
		return id;
	}
	
	//For Database and display (like APIHandler)
	public String getScore(String questions,String answers){
		return intsToString(getScore(getIds(questions),answers.split(",")));
	}
	public int[] getScore(int[] questions,String[] answers){
		int[] temp=new int[mc.scoreModules];
		int[] temp3=new int[mc.scoreModules];
		if(questions.length!=answers.length)
			return temp;
		int[] temp2=new int[questions.length];
		int index=0;
		for(int i=0;i<questions.length;i++){//Check Answer
			if(getQuestion(questions[i]).key.equalsIgnoreCase(answers[i])){
				temp2[index]=questions[i];
				index++;
			}
		}
		for(int i=0;i<index;i++){//Find every correct question module
			for(int j=1;j<=mc.scoreModules;j++){//Find in every module
				temp3[j-1]=getQuestionNumber(j);
				int[] set=mc.scoreModule[j];
				for(int k=0;k<set.length;k++){
					if(getQuestion(temp2[i]).type==set[k]){
						temp[j-1]++;
						break;
					}
				}
			}
		}
		for(int i=0;i<temp.length;i++){//get true score (correct / all)
			temp[i]=(int)(temp[i]/temp3[i]);
		}
		return temp;
	}
	
	//For Database and display (like APIHandler)
	public boolean isPass(String score){
		return isPass(getIds(score));
	}
	public boolean isPass(int[] score){
		if(score.length!=mc.scoreModules)
			return false;
		for(int i=0;i<score.length;i++){
			if(score[i]<mc.passingScore[i])
				return false;
		}
		return true;
	}
	
	//Tools begin
	private int getRandom(int min,int max){
		return (int)(Math.random()*(max-min+1))+min;
	}
	private boolean isUsed(int[] array,int test){
		for(int i=0;i < array.length;i++){
			if(array[i]==test){
				return true;
			}
		}
		return false;
	}
	private String intsToString(int[] array){
		StringBuilder sb=new StringBuilder();
		sb.append(array[0]);
		for(int i=1;i<array.length;i++){
			sb.append(",");
			sb.append(""+array[i]);
		}
		return sb.toString();
	}
	private int getQuestionNumber(int module){
		int temp=0;
		int[]temp2=mc.scoreModule[module];
		for(int j=0;j<temp2.length;j++){
			int type=temp2[j];
			if(type==0){
				temp=0;
				for(int i=1;i<=mc.questionTypes;i++){
					temp+=mc.questionNumber[i];
				}
				return temp;
			}
			temp+=mc.questionNumber[type];
		}
		return temp;
	}
}
