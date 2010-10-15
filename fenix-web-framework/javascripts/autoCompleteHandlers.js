function clearAutoComplete(input,inputText) {
	var id = escapeId(input.attr('id')) + "_AutoComplete";
	jQuery("input#" + id).val("");
	jQuery(input).parents("td").next("td:last").fadeOut();
}
function updateCustomValue(input,inputText) {
	id = escapeId(input.attr('id')) + "_OldValue";
	jQuery("input#" + id).val( inputText);
	jQuery("input#" + escapeId(input.attr('id')) + "_AutoComplete").val("custom");
	var name = escapeId(input.attr('id'));
	jQuery("input[name='" +  name + "']").val(jQuery("input#" + id).val());
}
function selectElement(input,inputText,element) {
	var id = escapeId(input.attr('id'));  
	jQuery("input#" + id + "_AutoComplete").val(element.attr('id')); 
	jQuery("input#" + id + "_OldValue").val( inputText);
	jQuery("input#" + id + "_AutoComplete").trigger("change");
}        
function escapeId(id) {          
	return id.replace(/\./g,"\\.").replace(/\:/g,"\\:");  
}
function showError(input,inputText) {
	jQuery(input).parents("td").next("td:last").fadeIn(300).html('<span>Ocorreu um erro no servidor, por favor volte a tentar.</span>'); 
}