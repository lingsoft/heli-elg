/*
Copyright 2020 Tommi Jauhiainen
Copyright 2022 University of Helsinki
Copyright 2022 Heidi Jauhiainen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
/*
 HeLI-OTS 1.4.
 
 If you use this program in producing scientific publications, please refer to:
 
 @inproceedings{heliots2022,
	 title = "{H}e{LI-OTS}, Off-the-shelf Language Identifier for Text",
	 author = "Jauhiainen, Tommi  and
	   Jauhiainen, Heidi  and
	   Lind{\'e}n, Krister",
	 booktitle = "Proceedings of the 13th Conference on Language Resources and Evaluation",
	 month = june,
	 year = "2022",
	 address = "Marseille, France",
	 publisher = "European Language Resources Association",
	 url = "http://www.lrec-conf.org/proceedings/lrec2022/pdf/2022.lrec-1.416.pdf",
	 pages = "3912--3922",
	 language = "English",
 }
 
 Producing and publishing this software has been partly supported by The Finnish Research Impact Foundation Tandem Industry Academia -funding in cooperation with Lingsoft.
 */

package eu.elg.heli.impl;

import java.io.*;
import java.util.*;

//import heli.HeLIResult;

public class HeLI {

    private static TreeMap<String, TreeMap<String, Float>> gramDict;
    private static TreeMap<String, TreeMap<String, Float>> wordDict;
	private static List<String> languageList = new ArrayList<String>();
    public static List<String> languageListFinal = new ArrayList<String>();
    private static List<String> languageListFinalOriginal = new ArrayList<String>();
	
// The following values are the ones used in Jauhiainen et al. 2017.

	private static float usedmonos = (float) 0.0000005;
	private static float usedbis = (float) 0.0000005;
	private static float usedtris = (float) 0.0000005;
	private static float usedquads = (float) 0.0000005;
	private static float usedcinqs = (float) 0.0000005;
	private static float usedsexts = (float) 0.0000005;
	private static float usedwords = (float) 0.0000005;
	
	private static float penaltyValue = (float) 7.0;
	
	private static int maxNgram = 6;
    
    private static boolean useConfidence = false;
    private static boolean filterMode = true;
    private static boolean printDirect = true;
    private static boolean readfromFile = false;
    private static boolean useRelevantLanguages = false;
    private static boolean printTopLanguages = false;
    private static boolean lastIsWord = true;
    private static boolean useOnlineLanguages = false;
    
    private static int numberTopLanguages = 5;
		
	public static void setup() {
        // useRelevantLanguages, relevantLanguages, numberTopLanguages
        String relevantLanguages = "";
        
        InputStream in = HeLI.class.getResourceAsStream("/languagelist");
        
		BufferedReader reader = null;
		
		try {
            reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String text = null;
			while ((text = reader.readLine()) != null) {
                if (!useRelevantLanguages) {
                    languageList.add(text);
                    if (!languageListFinal.contains(text.substring(0,3))) {
                        languageListFinal.add(text.substring(0,3));
                    }
                }
                else {
                    String[] relevantList = relevantLanguages.split(",");
                    if (Arrays.asList(relevantList).contains(text.substring(0,3))) {
                        languageList.add(text);
                        if (!languageListFinal.contains(text.substring(0,3))) {
                            languageListFinal.add(text.substring(0,3));
                        }
                    }
                }
			}
            languageListFinalOriginal = languageListFinal;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
		
			gramDict = new TreeMap<>();
			wordDict = new TreeMap<>();
			
			ListIterator gramiterator = languageList.listIterator();
			while(gramiterator.hasNext()) {
				Object element = gramiterator.next();
				String languageCode = (String) element;
				
				loadModel(usedmonos, languageCode, "LowGramModel1");
				loadModel(usedbis, languageCode, "LowGramModel2");
				loadModel(usedtris, languageCode, "LowGramModel3");
				loadModel(usedquads, languageCode, "LowGramModel4");
				loadModel(usedcinqs, languageCode, "LowGramModel5");
				loadModel(usedsexts, languageCode, "LowGramModel6");
				loadModel(usedwords, languageCode, "LowWordModel");
			}
		
    }
        
	private static TreeMap<String, TreeMap<String, Float>> ReadObjectFromFile(String filepath) {
	 
			try {
	 
				InputStream modelFile = null;
				
				modelFile = HeLI.class.getResourceAsStream(filepath);
				
				int bufferSize = 64 * 1024;
				ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(modelFile, bufferSize));
					 
				TreeMap<String, TreeMap<String, Float>> obj = (TreeMap<String, TreeMap<String, Float>>) objectIn.readObject();
	 
				objectIn.close();
				return obj;
	 
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}

	private static void loadModel(float usedFeatureRF, String languageCode, String modelType) {
        TreeMap<String, Float> tempDict;
		
        tempDict = new TreeMap<>();
	
        InputStream modelFile = null;
        
        modelFile = HeLI.class.getResourceAsStream("/LanguageModels/" + languageCode + "." + modelType);
	
		float totalFeatureNumber = 0;
		float langamount = 0;
	
		BufferedReader reader = null;
		try {
            reader = new BufferedReader(new InputStreamReader(modelFile));
			String text = null;
            
            text = reader.readLine();
            totalFeatureNumber = Float.parseFloat(text);
            
			while ((text = reader.readLine()) != null) {
                String[] line = text.split("\t");
                String gram = line[0];
                long amount = Long.parseLong(line[1]);
                
                if (amount/totalFeatureNumber > usedFeatureRF) {
                    tempDict.put(gram, (float) amount);
                    langamount = langamount + (float) amount;
                }
                else {
                    break;
                }
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}

        for (Map.Entry<String,Float> entry : tempDict.entrySet()) {
			Float probability = (float) -Math.log10(entry.getValue() / langamount);
            TreeMap <String, Float> kiepro = new TreeMap<>();
   			if (modelType.equals("LowWordModel")) {
                String word = " " + entry.getKey() + " ";
                if (wordDict.containsKey(word)) {
                    kiepro = wordDict.get(word);
                }
                kiepro.put(languageCode,probability);
                wordDict.put(word, kiepro);
			}
			else {
                if (gramDict.containsKey(entry.getKey())) {
                    kiepro = gramDict.get(entry.getKey());
                }
                kiepro.put(languageCode,probability);
				gramDict.put(entry.getKey(), kiepro);
			}
		}
	}
 
	public static List<HeLIResult> identifyLanguage(String mysteryText, List<String> languageCodes, int bestLangs) {
        if (languageCodes == null) {
            languageCodes = languageListFinal;
        }
        // TODO Fix languageList based on languageCodes (not all languages)
        //languageList = languageCodes;
        //System.out.println(languageList);
        //languageListFinal = languageListFinalOriginal;
        numberTopLanguages = bestLangs;
		mysteryText = mysteryText.toLowerCase();
		
		mysteryText = mysteryText.replaceAll("[^\\p{L}\\p{M}′'’´ʹािीुूृेैोौंँः् া ি ী ু ূ ৃ ে ৈ ো ৌ।্্্я̄\\u07A6\\u07A7\\u07A8\\u07A9\\u07AA\\u07AB\\u07AC\\u07AD\\u07AE\\u07AF\\u07B0\\u0A81\\u0A82\\u0A83\\u0ABC\\u0ABD\\u0ABE\\u0ABF\\u0AC0\\u0AC1\\u0AC2\\u0AC3\\u0AC4\\u0AC5\\u0AC6\\u0AC7\\u0AC8\\u0AC9\\u0ACA\\u0ACB\\u0ACC\\u0ACD\\u0AD0\\u0AE0\\u0AE1\\u0AE2\\u0AE3\\u0AE4\\u0AE5\\u0AE6\\u0AE7\\u0AE8\\u0AE9\\u0AEA\\u0AEB\\u0AEC\\u0AED\\u0AEE\\u0AEF\\u0AF0\\u0AF1]", " ");

		String mysteryText2 = "";
		int lastWasCJK = 0;
		int lastWasSpace = 0;
		int CJKcharacterAmount = 0;
		
		for (int charCounter = 0; charCounter < mysteryText.length(); charCounter++){
			char mysteryChar = mysteryText.charAt(charCounter);
			String mysteryCharSet;
			try {
				mysteryCharSet = Character.UnicodeBlock.of(mysteryChar).toString();
			}
			catch (Exception e) {
				return(List.of(new HeLIResult("und", 1.0f)));
			}
			if (mysteryCharSet.startsWith("CJK")) {
				if (lastWasCJK == 0 && lastWasSpace == 0) {
					mysteryText2 = mysteryText2 + " ";
				}
				lastWasCJK = 1;
				lastWasSpace = 0;
				CJKcharacterAmount++;
			}
			else {
				if (lastWasCJK == 1 && mysteryChar != ' ') {
					mysteryText2 = mysteryText2 + " ";
				}
				if (mysteryChar == ' ') {
					lastWasSpace = 1;
				}
				else {
					lastWasSpace = 0;
				}
				lastWasCJK = 0;
			}
			mysteryText2 = mysteryText2 + mysteryChar;
		}
		
		mysteryText = mysteryText2;
		
		mysteryText = mysteryText.replaceAll("  *", " ");
		
		mysteryText = mysteryText.replaceAll("^ ", "");
		
		int strLength = mysteryText.length();
		
		if (strLength == 0) {
			return(List.of(new HeLIResult("und", penaltyValue)));
		}

		String[] words = mysteryText.split(" ");
        
        Map<String, Float> languagePoints = new HashMap();
		
		ListIterator languageIterator = languageList.listIterator();
		while(languageIterator.hasNext()) {
			Object element = languageIterator.next();
			String scoredLanguage = (String) element;
			languagePoints.put(scoredLanguage, (float) 0.0);
		}
        
        int numberOfWords = words.length;
        int wordCounter = 1;
		
		for (String mysteryWord : words) {
			Boolean wordScored = false;
			
			Map<String, Float> wordScores = new HashMap();
			
            if (!lastIsWord && wordCounter == numberOfWords) {
                mysteryWord = " " + mysteryWord;
            }
            else {
                mysteryWord = " " + mysteryWord + " ";
            }
      
            TreeMap <String, Float> kiepro = new TreeMap<>();
			if (usedwords < 1) {
				if (wordDict.containsKey(mysteryWord)) {
					wordScored = true;
                    kiepro = wordDict.get(mysteryWord);
					languageIterator = languageList.listIterator();
					while(languageIterator.hasNext()) {
						Object element = languageIterator.next();
						String scoredLanguage = (String) element;
						if (kiepro.containsKey(scoredLanguage)) {
							wordScores.put(scoredLanguage, kiepro.get(scoredLanguage));
						}
						else {
							wordScores.put(scoredLanguage, penaltyValue);
						}
					}
				}
			}
			
			if (!wordScored) {
				languageIterator = languageList.listIterator();
				while(languageIterator.hasNext()) {
					Object element = languageIterator.next();
					String scoredLanguage = (String) element;
					wordScores.put(scoredLanguage, (float)  0.0);
				}
			}
			
			int t = maxNgram;
			while (t > 0) {
				if (wordScored) {
					break;
				}
				else {
					int pituus = mysteryWord.length();
					int x = 0;
					int grammaara = 0;
					if (pituus > (t-1)) {
						while (x < pituus - t + 1) {
							String gram = mysteryWord.substring(x,x+t);
							if (gramDict.containsKey(gram)) {
								grammaara = grammaara + 1;
								wordScored = true;
                                kiepro = gramDict.get(gram);
								languageIterator = languageList.listIterator();
								while(languageIterator.hasNext()) {
									Object element = languageIterator.next();
									String scoredLanguage = (String) element;
									if (kiepro.containsKey(scoredLanguage)) {
										wordScores.put(scoredLanguage, (wordScores.get(scoredLanguage)+kiepro.get(scoredLanguage)));
									}
									else {
										wordScores.put(scoredLanguage, (wordScores.get(scoredLanguage)+penaltyValue));
									}
								}
							}
							x = x + 1;
						}
					}
					if (wordScored) {
						languageIterator = languageList.listIterator();
						while(languageIterator.hasNext()) {
							Object element = languageIterator.next();
							String scoredLanguage = (String) element;
							wordScores.put(scoredLanguage, (wordScores.get(scoredLanguage)/grammaara));
						}
					}
				}
				t = t -1 ;
			}
			languageIterator = languageList.listIterator();
			while(languageIterator.hasNext()) {
				Object element = languageIterator.next();
				String scoredLanguage = (String) element;
				languagePoints.put(scoredLanguage, (languagePoints.get(scoredLanguage) + wordScores.get(scoredLanguage)));
			}
            
            wordCounter++;
		}
		
		String winningLanguage = "und";
 		Float smallestScore = penaltyValue + 1;
		languagePoints.put(winningLanguage, smallestScore);
        float wordNumber = words.length;
		
        Map<String, Float> languagePointsFinal = new HashMap();
        
		languageIterator = languageList.listIterator();
		while(languageIterator.hasNext()) {
			Object element = languageIterator.next();
			String scoredLanguage = (String) element;
            //System.out.println(scoredLanguage);
			languagePoints.put(scoredLanguage, (languagePoints.get(scoredLanguage)/wordNumber));
			if ((100/strLength*CJKcharacterAmount) > 50) {
				if (!scoredLanguage.equals("jpn") && !scoredLanguage.equals("kor") && !scoredLanguage.equals("cmn")) {
					languagePoints.put(scoredLanguage, (penaltyValue + 1));
				}
			}
            
            Float languageScore = languagePoints.get(scoredLanguage);
           
            if (languagePointsFinal.containsKey(scoredLanguage.substring(0,3))) {
                if (languageScore < languagePointsFinal.get(scoredLanguage.substring(0,3))) {
                    languagePointsFinal.put(scoredLanguage.substring(0,3), languageScore);
                }
            }
            else {
                languagePointsFinal.put(scoredLanguage.substring(0,3), languageScore);
            }
		}
                
        // Here we do a TreeMap, which has only three letter codes for languages
        
        TreeMap<Float, List<String>> HeLIScore = new TreeMap<>();
        
        //languageIterator = languageListFinal.listIterator();
        //languageIterator = languageList.listIterator();
        languageIterator = languageCodes.listIterator();
        while(languageIterator.hasNext()) {
            Object element = languageIterator.next();
            String scoredLanguage = (String) element;
            List<String> languages = new ArrayList<String>();
            Float languageScore = languagePointsFinal.get(scoredLanguage);
            if (HeLIScore.containsKey(languageScore)) {
                languages = HeLIScore.get(languageScore);
            }
            languages.add(scoredLanguage);
            HeLIScore.put(languageScore,languages);
        }
        
        if (HeLIScore.firstEntry().getValue().size() == 1) {
            winningLanguage = HeLIScore.firstEntry().getValue().get(0);
        }
        else {
            Random rand = new Random();
            int n = rand.nextInt(HeLIScore.firstEntry().getValue().size());
            winningLanguage = HeLIScore.firstEntry().getValue().get(n);
        }
        
        List<HeLIResult> result = new ArrayList<>();
        if (numberTopLanguages == 1) {
            Float languageScore = languagePointsFinal.get(winningLanguage);
            result.add(new HeLIResult(winningLanguage, languageScore));
        }
        else {
            int count = 1;
            entries: for(Map.Entry<Float, List<String>> entry : HeLIScore.entrySet()) {
                for(String lang : entry.getValue()) {
                    if(count <= numberTopLanguages) {
                        result.add(new HeLIResult(lang, entry.getKey()));
                    } else {
                        break entries;
                    }
                    count++;
                }
            }
        }
		return (result);
	}
}
