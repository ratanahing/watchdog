package interval;


import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import document.Document;
import document.DocumentType;
import document.IDocument;

import interval.activityCheckers.RunCallBack;
import interval.events.ClosingIntervalEvent;
import interval.events.IIntervalListener;
import interval.events.NewIntervalEvent;
import interval.events.IntervalNotifier;
import eclipseUIReader.IUIListener;
import eclipseUIReader.UIListener;
import eclipseUIReader.Events.DocumentAttentionEvent;
import eclipseUIReader.Events.DocumentNotifier;
import eclipseUIReader.Events.IDocumentAttentionListener;

public class IntervalKeeper extends IntervalNotifier implements IIntervalKeeper  {
	private ActiveInterval currentInterval;
	private IUIListener UIListener;
	
	private List<IInterval> recordedIntervals;
	
	public IntervalKeeper(){
		recordedIntervals= new LinkedList<IInterval>();
		
		DocumentNotifier.addMyEventListener(new IDocumentAttentionListener() {			
			
			@Override
			public void onDocumentActivated(final DocumentAttentionEvent evt) {
				if(currentInterval != null && currentInterval.getEditor() != evt.getChangedEditor()){
					closeCurrentInterval();					
				}
				
				//create a new active interval when doc is new
				if(currentInterval == null){
					createNewInterval(evt);	
				}
			}
						
			
			@Override
			public void onDocumentDeactivated(DocumentAttentionEvent evt) {
				if(currentInterval != null && evt.getChangedEditor() == currentInterval.getEditor()){										
					closeCurrentInterval();
				}				
			}
			
		});
		UIListener = new UIListener();
		UIListener.attachListeners();		
	}
	
	private void closeCurrentInterval() {	
		IDocument doc = new Document(currentInterval.getEditor().getTitle(), DocumentType.PRODUCTION);
		RecordedInterval recordedInterval = new RecordedInterval(doc, currentInterval.getTimeOfCreation(), new Date());
		recordedIntervals.add(recordedInterval);
		currentInterval.getTimer().cancel(); //stop the timer that checks for changes in a doc
		currentInterval = null;
		IntervalNotifier.fireOnClosingInterval(new ClosingIntervalEvent(recordedInterval));
	}
	
	private void createNewInterval(final DocumentAttentionEvent evt) {				
		ActiveInterval activeInterval = new ActiveInterval(evt.getChangedEditor());
		currentInterval = activeInterval;
		activeInterval.start(3000, new RunCallBack() {					
			@Override
			public void onInactive() {
				closeCurrentInterval();
			}
		});
		IntervalNotifier.fireOnNewInterval(new NewIntervalEvent(activeInterval));
	}
	
	@Override
	public void addIntervalListener(IIntervalListener listener){
		IntervalNotifier.addMyEventListener(listener);
	}
	
	@Override
	public void removeIntervalListener(IIntervalListener listener){
		IntervalNotifier.removeMyEventListener(listener);
	}
	
	@Override
	public List<IInterval> getRecordedIntervals(){
		return recordedIntervals;
	}
}
