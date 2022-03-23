package eu.elg.heli.impl;
/*
Copyright 2022 Ian Roberts

Based on the original command-line HeLI.java (see HeLI.java.original)
from https://zenodo.org/record/6077089.  Original copyright notice follows.

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
 HeLI-OTS 1.3.
 
 If you use this program in producing scientific publications, please refer to:
 
 @inproceedings{jauhiainen-etal-2017-evaluation,
     title = "Evaluation of language identification methods using 285 languages",
     author = "Jauhiainen, Tommi  and
       Lind{\'e}n, Krister  and
       Jauhiainen, Heidi",
     booktitle = "Proceedings of the 21st Nordic Conference on Computational Linguistics",
     month = may,
     year = "2017",
     address = "Gothenburg, Sweden",
     publisher = "Association for Computational Linguistics",
     url = "https://www.aclweb.org/anthology/W17-0221",
     pages = "183--191",
 }
 
 Producing and publishing this software has been partly supported by The Finnish Research Impact Foundation Tandem Industry Academia -funding in cooperation with Lingsoft.
 */

import java.io.*;
import java.util.*;

public class HeLI {

    private static TreeMap<String, TreeMap<String, Float>> gramDict;
    private static TreeMap<String, TreeMap<String, Float>> wordDict;
	private static List<String> languageList = new ArrayList<String>();
    public static final List<String> languageListFinal = new ArrayList<String>();
    private static List<String> languageListFinalOriginal = new ArrayList<String>();
		// IR - added 3-to-2 code mapping
		private static HashMap<String, String> languages3to2 = new HashMap<String, String>();
		static {
			try(InputStream str = HeLI.class.getResourceAsStream("/languages-3-to-2.csv");
					BufferedReader rdr = new BufferedReader(new InputStreamReader(str, "UTF-8"))) {
				String line;
				while((line = rdr.readLine()) != null) {
					int comma = line.indexOf(',');
					languages3to2.put(line.substring(0, comma), line.substring(comma+1));
				}
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		// end 3-to-2 code mapping

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
    
    private static boolean useRelevantLanguages = false;
    private static boolean printTopLanguages = false;
    private static boolean lastIsWord = true;

    private static int numberTopLanguages = 1;
		
	public static void setup(String... args) {
        
        String allHelp = "HeLI off-the-shelf language identifier with language models for 200 languages (http://urn.fi/urn:nbn:fi:lb-2021062801). The program will read the <infile> and classify the language of each line as one of the 200 languages it knows and writes the results, one ISO 639-3 code per line, into file <outfile>. It can identify c. 3000 sentences per second using one core on a 2021 laptop and around 3 gigabytes of memory.\n" +
        
        "Producing and publishing this software has been partly supported by The Finnish Research Impact Foundation Tandem Industry Academia -funding in cooperation with Lingsoft.\n" +
        "You can give the name of the text file to be identified after -r and the name of a new file to write the results in after -w.\n\n"+
        "Usage: java -jar HeLI.jar -r <infile> -w <outfile>\n\n"+
        "You can use the -c option to make the program print a confidence score for the identificatiuon after each language code.\n\n"+
        "Usage: java -jar HeLI.jar -c -r <infile> -w <outfile>\n\n"+
        "If you omit both of the filenames (filter mode), the program will read the standard input one line at a time and write the result to standard output.\n\n"+
        "You can give the list of comma-separated ISO 639-3 identifiers for relevant languages after the -l option.\n\n"+
        "Usage: java -jar HeLI.jar -r <infile> -w <outfile> -l fin,swe,eng\n\n" +
        "You can give the number of top-scored languages to print after -t option. (overrides confidence)\n\n"+
        "Usage: java -jar HeLI.jar -r <infile> -w <outfile> -l fin,swe,eng -t 2\n\n" +
        "You can set the program to accept online command using the -ol option.\n\n"+
        "When the -ol option is active, the identifier will look for online commands from the beginning of the input lines in the filter mode.\n\n"+
        "If \"!HeLI-sllf:\" is encountered in the beginning, the relevant languages change to the list following \":\".\n\n"+
        "Usage: !HeLI-sllf:fin,swe,eng\n\n" +
        "If \"!HeLI-rllf:\" is encountered in the beginning, the relevant languages will reset to those chosen when the program was launched.\n\n"+
        "Usage: !HeLI-rllf:\n\n";

        String mysteryTexts = "";
        String resultsFile = "";
        String relevantLanguages = "";
        
        int processargs = 0;
        
        while (args.length > processargs) {
            int oldprocessargs = processargs;
            if (args.length > processargs && args[processargs].equals("-h")) {
                System.out.println(allHelp);
                System.exit(0);
            }
            if (args.length > processargs && args[processargs].equals("-p")) {
                lastIsWord = false;
                processargs++;
            }
            if (args.length > processargs && args[processargs].equals("-l")) {
                if (args.length > processargs+1) {
                    useRelevantLanguages = true;
                    relevantLanguages = args[processargs+1];
                    processargs++;
                    processargs++;
                }
                else {
                    System.out.println("Invalid arguments.");
                    System.out.println(allHelp);
                    System.exit(0);
                }
            }
            if (args.length > processargs && args[processargs].equals("-t")) {
                if (args.length > processargs+1) {
                    printTopLanguages = true;
                    numberTopLanguages = Integer.parseInt(args[processargs+1]);
                    processargs++;
                    processargs++;
                }
                else {
                    System.out.println("Invalid arguments.");
                    System.out.println(allHelp);
                    System.exit(0);
                }
            }
            if (oldprocessargs == processargs) {
                System.out.println("Invalid arguments.");
                System.out.println(allHelp);
                System.exit(0);
            }
        }
        
        File file;

        InputStream in = HeLI.class.getResourceAsStream("/languagelist");
        
		BufferedReader reader = null;
		
		try {
            reader = new BufferedReader(new InputStreamReader(in));
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

    // main processing loop disabled
    /*
        if (!filterMode && readfromFile) {
            file = new File(mysteryTexts);
            
            reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line = "";
                
                if (!printDirect) {
                    file = new File(resultsFile);
                    file.createNewFile();
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter(file, true));
                    }
                    catch (Exception e) {
                        System.out.println("Error while creating writer: "+e.getMessage());
                    }
                    
                    while ((line = reader.readLine()) != null) {
                        try {
                            writer.write(HeLI.identifyLanguage(line)+"\n");
                        }
                        catch (Exception e) {
                            System.out.println("Error while trying to write: "+e.getMessage());
                        }
                    }
                    
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                else {
                    while ((line = reader.readLine()) != null) {
                        System.out.println(HeLI.identifyLanguage(line));
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
        }
        else {
            Scanner heliLoop = new Scanner(System.in);
            while (heliLoop.hasNextLine()) {
                String line = heliLoop.nextLine();
                boolean lineWasCommand = false;
                if (useOnlineLanguages) {
                    if (line.length() > 10 && line.substring(0,11).equals("!HeLI-sllf:")) {
                        String[] mysteryCommand = line.split(":");
                        String[] relevantList = mysteryCommand[1].split(",");
                        
                        languageListFinal = new ArrayList<String>();
                        
                        ListIterator relevantIterator = Arrays.asList(relevantList).listIterator();
                        while(relevantIterator.hasNext()) {
                            Object element = relevantIterator.next();
                            String relevantLanguage = (String) element;
                            if (languageListFinalOriginal.contains(relevantLanguage)) {
                                languageListFinal.add(relevantLanguage);
                            }
                        }
                        if (languageListFinal.size() == 0) {
                            languageListFinal = languageListFinalOriginal;
                        }
                        lineWasCommand = true;
                    }
                    if (line.length() > 10 && line.substring(0,11).equals("!HeLI-rllf:")) {
                        languageListFinal = languageListFinalOriginal;
                        lineWasCommand = true;
                    }
                }
                if (!lineWasCommand) {
                    System.out.println(HeLI.identifyLanguage(line));
                }
            }
        }
     */
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
    
	public static List<HeLIResult> identifyLanguage(String mysteryText, List<String> languageCodes) {
    if(languageCodes == null) {
      // default set of language codes
      languageCodes = languageListFinal;
    }
        
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
				return(Arrays.asList(new HeLIResult("xxx", null, 1.0f)));
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
			return(Arrays.asList(new HeLIResult("xxx", null, 1.0f)));
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
		
		String winningLanguage = "xxx";
 		Float smallestScore = penaltyValue + 1;
        float wordNumber = words.length;
		
        Map<String, Float> languagePointsFinal = new HashMap();
        
		languageIterator = languageList.listIterator();
		while(languageIterator.hasNext()) {
			Object element = languageIterator.next();
			String scoredLanguage = (String) element;
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
            result.add(new HeLIResult(winningLanguage, languages3to2.get(winningLanguage.substring(0, 3)), 1.0f));
        }
        else {

          //float confidence = 0;
          int count = 1;
          entries: for(Map.Entry<Float, List<String>> entry : HeLIScore.entrySet()) {
            /*
            if (count == 2 && HeLIScore.firstEntry().getValue().size() == 1) {
                confidence = entry.getKey() - languagePointsFinal.get(winningLanguage);
            }*/
            for(String lang : entry.getValue()) {
              if(count <= numberTopLanguages) {
                result.add(new HeLIResult(lang, languages3to2.get(lang.substring(0, 3)), entry.getKey()));
              } else {
                break entries;
              }
              count++;
            }
          }
        }

        /*
        if (useConfidence && numberTopLanguages == 1) {
            result = result + "\t" + confidence;
        }
         */
        
		return (result);
	}
}